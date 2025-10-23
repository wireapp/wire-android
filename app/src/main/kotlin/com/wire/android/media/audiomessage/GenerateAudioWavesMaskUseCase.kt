/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.media.audiomessage

import android.content.Context
import com.waz.audioeffect.AudioEffect
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateAudioWavesMaskUseCase @Inject constructor(
    private val dispatchers: DispatcherProvider,
    @ApplicationContext private val context: Context,
) {
    /**
     * Note: This UseCase can't be tested as we cannot mock `AudioEffect` from AVS.
     * Generates audio waves mask for the given file path.
     *
     * @param filePath the path to the audio file.
     * @return array of integers representing the audio waves mask.
     */
    suspend operator fun invoke(filePath: String): List<Int>? = withContext(dispatchers.io()) {
        appLogger.i("[$TAG] -> Start generating audio waves mask")

        val audioEffect = AudioEffect(context)
        val wavesMask = audioEffect.amplitudeGenerate(filePath, WAVE_MAX, WAVES_AMOUNT)

        wavesMask?.toList().also {
            if (wavesMask == null) {
                appLogger.w("[$TAG] -> There was an issue with generating audio waves mask.")
            } else {
                appLogger.i("[$TAG] -> Audio waves mask generated successfully.")
            }
        }
    }

    private companion object Companion {
        const val TAG = "GenerateAudioWavesMaskUseCase"
        private const val WAVES_AMOUNT = 75
        private const val WAVE_MAX = 32
    }
}
