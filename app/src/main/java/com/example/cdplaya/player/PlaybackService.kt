package com.example.cdplaya.player

import android.app.PendingIntent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.cdplaya.MainActivity
import com.example.cdplaya.data.Song
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private lateinit var playerStateStorage: PlayerStateStorage
    private val checkpointHandler = Handler(Looper.getMainLooper())
    private val checkpointRunnable = object : Runnable {
        override fun run() {
            saveServicePlaybackState()
            if (::player.isInitialized && player.isPlaying) {
                checkpointHandler.postDelayed(
                    this,
                    PlaybackStateCheckpointPolicy.DEFAULT_INTERVAL_MILLIS
                )
            }
        }
    }
    private val persistenceListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            saveServicePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            checkpointHandler.removeCallbacks(checkpointRunnable)
            if (isPlaying) {
                checkpointHandler.postDelayed(
                    checkpointRunnable,
                    PlaybackStateCheckpointPolicy.DEFAULT_INTERVAL_MILLIS
                )
            } else {
                saveServicePlaybackState()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                saveServicePlaybackState()
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            saveServicePlaybackState()
        }
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(buildBrowseTree().toMediaItem(), params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = buildBrowseTree().findNode(parentId)?.children.orEmpty()
            val fromIndex = (page * pageSize).coerceAtMost(children.size)
            val toIndex = (fromIndex + pageSize).coerceAtMost(children.size)

            return Futures.immediateFuture(
                LibraryResult.ofItemList(
                    children.subList(fromIndex, toIndex).map { it.toMediaItem() },
                    params
                )
            )
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            if (mediaItems.all { it.localConfiguration != null }) {
                return super.onSetMediaItems(
                    mediaSession,
                    controller,
                    mediaItems,
                    startIndex,
                    startPositionMs
                )
            }

            val tree = buildBrowseTree()
            val selectedIndex = startIndex.takeIf { it in mediaItems.indices } ?: 0
            val requestedId = mediaItems.getOrNull(selectedIndex)?.mediaId.orEmpty()
            val selectedNode = tree.findNode(requestedId)
            val contextSongs = tree.findParent(requestedId)
                ?.children
                ?.mapNotNull { it.song }
                .orEmpty()
            val selectedSong = selectedNode?.song

            if (selectedSong != null) {
                val playbackContext = contextSongs.ifEmpty { listOf(selectedSong) }
                PlaybackLibraryBridge.playSelectedSong(selectedSong, playbackContext)
                val resolvedItems = playbackContext.map { song -> song.toPlayableMediaItem() }
                val resolvedIndex = resolvedItems.indexOfFirst {
                    it.mediaId == selectedSong.id.toString()
                }
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        resolvedItems,
                        resolvedIndex,
                        startPositionMs
                    )
                )
            }

            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        playerStateStorage = PlayerStateStorage(this)
        player.addListener(persistenceListener)

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, libraryCallback)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        checkpointHandler.removeCallbacks(checkpointRunnable)
        saveServicePlaybackState()
        player.removeListener(persistenceListener)
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }

    private fun saveServicePlaybackState() {
        if (!::player.isInitialized || !::playerStateStorage.isInitialized) return
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
        playerStateStorage.saveServicePlaybackState(
            currentSongId = songId,
            currentPosition = player.currentPosition
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt(),
            repeatMode = repeatMode
        )
    }

    private fun buildBrowseTree(): AutoBrowseNode {
        return buildAndroidAutoBrowseTree(PlaybackLibraryBridge.songs)
    }

    private fun AutoBrowseNode.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setIsBrowsable(children.isNotEmpty())
            .setIsPlayable(song != null)
            .setArtworkUri(song?.albumArtUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Song.toPlayableMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .build()
            )
            .build()
    }

}
