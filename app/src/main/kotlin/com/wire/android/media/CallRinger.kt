package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.wire.android.appLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRinger @Inject constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    init {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        am?.isSpeakerphoneOn = false
        am?.mode = AudioManager.MODE_IN_COMMUNICATION;
    }

    private fun createMediaPlayer(resource: Int, isLooping: Boolean) {
        mediaPlayer = MediaPlayer.create(
            context, resource,
            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(), 0
        )
        mediaPlayer?.isLooping = isLooping
    }

    fun ring(resource: Int, isLooping: Boolean = true) {
        mediaPlayer?.release()
        createMediaPlayer(resource, isLooping)
        mediaPlayer?.start()
    }

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true)
                mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: IllegalStateException) {
            appLogger.e("There was an error while stopping ringing", e)
        }
    }
}
