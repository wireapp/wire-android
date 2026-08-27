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
import com.wire.android.di.ApplicationContext
import com.wire.android.di.metro.MetroSessionScope
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.media.recording.AudioEffectsProcessor
import com.wire.media.recording.AudioEffectsRequest
import com.wire.media.recording.RecordingCapabilityResult
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.Path

@Inject
@SingleIn(MetroSessionScope::class)
@ContributesBinding(MetroSessionScope::class, binding = binding<AudioEffectsProcessor>())
class GenerateAudioFileWithEffectsUseCase(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val fileSystem: KaliumFileSystem,
) : AudioEffectsProcessor {
    /**
     * Note: This UseCase can't be tested as we cannot mock `AudioEffect` from AVS.
     * Generates audio file with effects on received path from the original file path.
     *
     * @return the generated path, or an explicit failure after cleaning a partial output.
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun process(request: AudioEffectsRequest): RecordingCapabilityResult<Path> =
        withContext(dispatchers.io()) {
            appLogger.i("[$TAG] -> Start generating audio file with effects")

            try {
                val audioEffect = AudioEffect(context)
                val audioEffectsResult = audioEffect.applyEffectWav(
                    request.source.toString(),
                    request.destination.toString(),
                    AudioEffect.AVS_AUDIO_EFFECT_VOCODER_MED,
                    true,
                )

                if (audioEffectsResult > -1) {
                    appLogger.i("[$TAG] -> Audio file with effects generated successfully.")
                    RecordingCapabilityResult.Success(request.destination)
                } else {
                    appLogger.w("[$TAG] -> There was an issue with generating audio file with effects.")
                    deleteOutput(request.destination)
                    RecordingCapabilityResult.Failure("audio_effect_failed")
                }
            } catch (cancelled: CancellationException) {
                deleteOutput(request.destination)
                throw cancelled
            } catch (error: Exception) {
                appLogger.e("[$TAG] -> Audio effect generation failed", error)
                deleteOutput(request.destination)
                RecordingCapabilityResult.Failure(error.message)
            }
        }

    private fun deleteOutput(path: Path) {
        if (fileSystem.exists(path)) fileSystem.delete(path)
    }

    private companion object {
        const val TAG = "GenerateAudioFileWithEffectsUseCase"
    }
}
