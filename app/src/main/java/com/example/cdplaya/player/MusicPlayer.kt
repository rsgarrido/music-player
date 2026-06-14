package com.example.cdplaya.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.cdplaya.data.Song
import com.google.common.util.concurrent.ListenableFuture

class MusicPlayer(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var currentSong: Song? = null

    var onSongCompleted: (() -> Unit)? = null
    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null

    fun connect(onConnected: (() -> Unit)? = null) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        val future = MediaController.Builder(context, sessionToken)
            .buildAsync()

        controllerFuture = future

        future.addListener(
            {
                controller = future.get()

                controller?.addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                onSongCompleted?.invoke()
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            onPlaybackStateChanged?.invoke(isPlaying)
                        }
                    }
                )

                onConnected?.invoke()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun playSong(
        song: Song,
        shouldStart: Boolean = true,
        startPosition: Int = 0
    ) {
        val playerController = controller ?: return

        currentSong = song

        val mediaItem = song.toMediaItem()

        playerController.setMediaItem(mediaItem)
        playerController.prepare()

        if (startPosition > 0) {
            playerController.seekTo(startPosition.toLong())
        }

        if (shouldStart) {
            playerController.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun stop() {
        controller?.stop()
        currentSong = null
    }

    fun isPlaying(): Boolean {
        return controller?.isPlaying == true
    }

    fun getCurrentSong(): Song? {
        return currentSong
    }

    fun getCurrentPosition(): Int {
        return controller?.currentPosition?.toInt() ?: 0
    }

    fun getDuration(): Int {
        val duration = controller?.duration ?: 0L

        return if (duration > 0) {
            duration.toInt()
        } else {
            0
        }
    }

    fun seekTo(position: Int) {
        controller?.seekTo(position.toLong())
    }

    fun release() {
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }

        controllerFuture = null
        controller = null
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}