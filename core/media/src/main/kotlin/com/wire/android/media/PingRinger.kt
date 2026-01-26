/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
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
            val hasVibratePermission = context.checkSelfPermission(Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED
            if (!hasVibratePermission) {
                return
            }
            vibrator?.let {
                if (!it.hasVibrator()) {
                    return
                }
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                val ringerMode = audioManager?.ringerMode
                if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    @SuppressLint("MissingPermission")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, DO_NOT_REPEAT))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(VIBRATE_PATTERN, DO_NOT_REPEAT)
                    }
                }
            }
        }
    }

    companion object {
        val VIBRATE_PATTERN: LongArray = longArrayOf(50, 50, 100, 150, 200, 250)
        const val DO_NOT_REPEAT = -1 // Do not repeat.
    }
}
