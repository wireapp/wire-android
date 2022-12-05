package com.wire.android.media

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.wire.android.appLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRinger @Inject constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    init {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        am?.isSpeakerphoneOn = false
        am?.mode = AudioManager.MODE_IN_COMMUNICATION
        initVibrator()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun createMediaPlayer(resource: Int, isLooping: Boolean) {
        mediaPlayer = MediaPlayer.create(
            context,
            resource,
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build(),
            0
        )
        mediaPlayer?.isLooping = isLooping
    }

    fun ring(
        resource: Int,
        isLooping: Boolean = true,
        isIncomingCall: Boolean = true
    ) {
        stop()
        vibrateIfNeeded(isIncomingCall)
        createMediaPlayer(resource, isLooping)
        appLogger.i("Starting ringing | isIncomingCall: $isIncomingCall");
        mediaPlayer?.start()
    }


    @Suppress("NestedBlockDepth")
    private fun vibrateIfNeeded(isIncomingCall: Boolean) {
        if (isIncomingCall) {
            vibrator?.let {
                if (!it.hasVibrator()) {
                    appLogger.i("Device does not support vibration");
                    return
                }
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                val ringerMode = audioManager?.ringerMode
                if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    appLogger.i("Starting vibration");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, VibrationEffect.EFFECT_DOUBLE_CLICK))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(VIBRATE_PATTERN, VibrationEffect.EFFECT_DOUBLE_CLICK)
                    }
                } else {
                    appLogger.i("Skipping vibration");
                }
            }
        }
    }

    fun stop() {
        appLogger.i("Stopping ringing");
        try {
            vibrator?.cancel()
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }

            mediaPlayer = null
        } catch (e: IllegalStateException) {
            appLogger.e("There was an error while stopping ringing", e)
        }
    }

    companion object {
        private val VIBRATE_PATTERN = longArrayOf(0, 1000, 1000)
    }
}
