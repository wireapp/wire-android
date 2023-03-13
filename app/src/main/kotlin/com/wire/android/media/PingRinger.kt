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
class PingRinger @Inject constructor(private val context: Context) {

    private var vibrator: Vibrator? = null

    init {
        initVibrator()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(VIBRATOR_SERVICE) as Vibrator?
        }
    }

    private fun createMediaPlayer(resource: Int): MediaPlayer? {
        val player = MediaPlayer.create(
            context,
            resource,
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build(),
            0
        )
        return if (player != null) {
            player.isLooping = false
            player
        } else {
            null
        }
    }

    fun ping(
        resource: Int,
        isReceivingPing: Boolean = true
    ) {
        vibrateIfNeeded(isReceivingPing)
        createMediaPlayer(resource)?.start()
    }

    @Suppress("NestedBlockDepth")
    private fun vibrateIfNeeded(isReceivingPing: Boolean) {
        if (isReceivingPing) {
            vibrator?.let {
                if (!it.hasVibrator()) {
                    appLogger.i("Device does not support vibration")
                    return
                }
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                val ringerMode = audioManager?.ringerMode
                if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    appLogger.i("Starting vibration")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(VIBRATE_PATTERN, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                } else {
                    appLogger.i("Skipping vibration")
                }
            }
        }
    }

    companion object {
        private val VIBRATE_PATTERN: LongArray = longArrayOf(0L, 1000L, 1000L, 1000L)
    }
}
