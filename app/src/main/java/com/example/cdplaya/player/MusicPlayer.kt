package com.example.cdplaya.player

import android.content.Context
import android.media.MediaPlayer
import com.example.cdplaya.data.Song

class MusicPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Song? = null

    var onSongCompleted: (() -> Unit)? = null

    fun playSong(song: Song) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(context, song.uri)

        mediaPlayer?.setOnCompletionListener {
            onSongCompleted?.invoke()
        }

        mediaPlayer?.start()

        currentSong = song
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentSong = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun getCurrentSong(): Song? {
        return currentSong
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }
}