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
package com.wire.android.ui.home.messagecomposer.recordaudio

import android.content.Context
import com.waz.audioeffect.AudioEffect
import com.wire.android.appLogger
import javax.inject.Singleton
import javax.inject.Inject

@Singleton
class GenerateAudioFileWithEffectsUseCase @Inject constructor() {
    /**
     * Note: This UseCase can't be tested as we cannot mock `AudioEffect` from AVS.
     * Generates audio file with effects on received path from the original file path.
     *
     * @return Unit, as the content of audio with effects will be saved directly to received file path.
     */
    operator fun invoke(
        context: Context,
        originalFilePath: String,
        effectsFilePath: String
    ) {
        val audioEffectsResult = AudioEffect(context)
            .applyEffectM4A(
                originalFilePath,
                effectsFilePath,
                AudioEffect.AVS_AUDIO_EFFECT_VOCODER_MED,
                true
            )

        if (audioEffectsResult > -1) {
            appLogger.i("[$TAG] -> Audio file with effects generated successfully.")
        } else {
            appLogger.w("[$TAG] -> There was an issue with generating audio file with effects.")
        }
    }

    private companion object {
        const val TAG = "GenerateAudioFileWithEffectsUseCase"
    }
}
