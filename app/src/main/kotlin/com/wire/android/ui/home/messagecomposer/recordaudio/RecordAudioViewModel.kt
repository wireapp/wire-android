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
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waz.audioeffect.AudioEffect
import com.wire.android.appLogger
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.util.AUDIO_MIME_TYPE
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.getAudioLengthInMs
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import java.io.File
import javax.inject.Inject
import kotlin.io.path.deleteIfExists

@Suppress("TooManyFunctions")
@HiltViewModel
class RecordAudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val audioMediaRecorder: AudioMediaRecorder
) : ViewModel() {

    private var state: RecordAudioState by mutableStateOf(RecordAudioState())

    private var hasOngoingCall: Boolean = false

    private val infoMessage = MutableSharedFlow<UIText>()

    fun getButtonState(): RecordAudioButtonState = state.buttonState

    fun getDiscardDialogState(): RecordAudioDialogState = state.discardDialogState

    fun getPermissionsDeniedDialogState(): RecordAudioDialogState =
        state.permissionsDeniedDialogState

    fun getMaxFileSizeReachedDialogState(): RecordAudioDialogState =
        state.maxFileSizeReachedDialogState

    fun getOutputFile(): File? = state.originalOutputFile

    fun getAudioState(): AudioState = state.audioState

    fun getInfoMessage(): SharedFlow<UIText> = infoMessage.asSharedFlow()

    init {
        observeAudioPlayerState()

        viewModelScope.launch {
            launch {
                observeAudioFileSize()
            }
            launch {
                observeScreenState()
            }
            launch {
                observeUserIsInCall()
            }
        }
    }

    private suspend fun observeAudioFileSize() {
        audioMediaRecorder.getMaxFileSizeReached().collect { recordAudioDialogState ->
            stopRecording()
            state = state.copy(
                maxFileSizeReachedDialogState = recordAudioDialogState
            )
        }
    }

    private suspend fun observeUserIsInCall() {
        observeEstablishedCalls().collect {
            hasOngoingCall = it.isNotEmpty()
        }
    }

    private suspend fun observeScreenState() {
        currentScreenManager.observeCurrentScreen(viewModelScope).collect { currentScreen ->
            if (state.buttonState == RecordAudioButtonState.RECORDING &&
                currentScreen == CurrentScreen.InBackground
            ) {
                stopRecording()
            }
        }
    }

    private fun observeAudioPlayerState() {
        viewModelScope.launch {
            recordAudioMessagePlayer.audioMessageStateFlow.collect {
                state = state.copy(
                    audioState = it
                )
            }
        }
    }

    fun startRecording() {
        if (hasOngoingCall) {
            viewModelScope.launch {
                infoMessage.emit(RecordAudioInfoMessageType.UnableToRecordAudioCall.uiText)
            }
        } else {
            viewModelScope.launch {
                val assetSizeLimit = getAssetSizeLimit(false)
                audioMediaRecorder.setUp(assetSizeLimit)
                if (audioMediaRecorder.startRecording()) {
                    state = state.copy(
                        originalOutputFile = audioMediaRecorder.originalOutputFile,
                        effectsOutputFile = audioMediaRecorder.effectsOutputFile,
                        dusanAudioFile = audioMediaRecorder.dusanAudioPath,
                        buttonState = RecordAudioButtonState.RECORDING
                    )
                } else {
                    infoMessage.emit(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText)
                }
            }
        }
    }

    fun stopRecording() {
        if (state.buttonState == RecordAudioButtonState.RECORDING) {
            audioMediaRecorder.stop()

            state = state.copy(
                buttonState = RecordAudioButtonState.READY_TO_SEND,
                audioState = AudioState.DEFAULT.copy(
                    totalTimeInMs = AudioState.TotalTimeInMs.Known(
                        state.originalOutputFile?.let {
                            getAudioLengthInMs(
                                dataPath = it.path.toPath(),
                                mimeType = AUDIO_MIME_TYPE
                            ).toInt()
                        } ?: 0
                    )
                )
            )
        }
        audioMediaRecorder.release()

//        val result = AudioEffect(context)
//            .applyEffectM4A(
//                state.originalOutputFile!!.path,
//                state.effectsOutputFile!!.path,
//                AudioEffect.AVS_AUDIO_EFFECT_VOCODER_MED,
//                true
//            )
//
//        appLogger.d("AUDIO_EFFECTS_stopRecording -> Result is : $result")
//        if (result > -1) {
//            appLogger.d("AUDIO_EFFECTS_stopRecording -> Effects Audio File")
//        } else {
//            appLogger.d("AUDIO_EFFECTS_stopRecording -> NULL Audio File")
//        }
    }

    fun showDiscardRecordingDialog(onCloseRecordAudio: () -> Unit) {
        when (state.buttonState) {
            RecordAudioButtonState.ENABLED -> onCloseRecordAudio()
            RecordAudioButtonState.RECORDING,
            RecordAudioButtonState.READY_TO_SEND -> {
                state = state.copy(
                    discardDialogState = RecordAudioDialogState.Shown
                )
            }
        }
    }

    fun onDismissDiscardDialog() {
        state = state.copy(
            discardDialogState = RecordAudioDialogState.Hidden
        )
    }

    fun showPermissionsDeniedDialog() {
        state = state.copy(
            permissionsDeniedDialogState = RecordAudioDialogState.Shown
        )
    }

    fun onDismissPermissionsDeniedDialog() {
        state = state.copy(
            permissionsDeniedDialogState = RecordAudioDialogState.Hidden
        )
    }

    fun onDismissMaxFileSizeReachedDialog() {
        state = state.copy(
            maxFileSizeReachedDialogState = RecordAudioDialogState.Hidden
        )
    }

    fun discardRecording(onCloseRecordAudio: () -> Unit) {
        viewModelScope.launch {
            state.originalOutputFile?.toPath()?.deleteIfExists()
            recordAudioMessagePlayer.stop()
            recordAudioMessagePlayer.close()
            state = state.copy(
                buttonState = RecordAudioButtonState.ENABLED,
                discardDialogState = RecordAudioDialogState.Hidden,
                originalOutputFile = null
            )
            onCloseRecordAudio()
        }
    }

    fun sendRecording(
        shouldApplyEffects: Boolean,
        onAudioRecorded: (UriAsset) -> Unit,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            recordAudioMessagePlayer.stop()
            recordAudioMessagePlayer.close()
            state.originalOutputFile?.let { originalFile ->
                appLogger.d("DUSAN -> ${state.dusanAudioFile?.path}")
                val resultFile: Uri? = if (shouldApplyEffects) {
                    val result = AudioEffect(context)
                        .applyEffectM4A(
                            state.dusanAudioFile!!.path, // originalFile.path,
                            state.effectsOutputFile!!.path,
                            AudioEffect.AVS_AUDIO_EFFECT_VOCODER_MED,
                            true
                        )

                    appLogger.d("AUDIO_EFFECTS -> Result is : $result")
                    appLogger.d("AUDIO_EFFECTS -> File name is : ${state.effectsOutputFile?.name}")
                    appLogger.d("AUDIO_EFFECTS -> File path is : ${state.effectsOutputFile?.path}")
                    if (result > -1) {
                        appLogger.d("AUDIO_EFFECTS -> Effects Audio File")
                        state.effectsOutputFile!!.toUri()
                    } else {
                        appLogger.d("AUDIO_EFFECTS -> NULL Audio File")
                        null
                    }
                } else {
                    appLogger.d("AUDIO_EFFECTS -> Original Audio File")
                    originalFile.toUri()
                }

                resultFile?.let { audioFileUri ->
                    appLogger.d("AUDIO_EFFECTS -> Sending Audio File")
                    onAudioRecorded(
                        UriAsset(
                            uri = originalFile.toUri(), // audioFileUri,
                            saveToDeviceIfInvalid = false
                        )
                    )
                    onComplete()
                } ?: appLogger.d("AUDIO_EFFECTS -> Error on resultFile is null")

                // TODO(RecordAudio): Question: Should we remove the file here as well?
            }
        }
    }

    fun onPlayAudio() {
        state.originalOutputFile?.let { audioFile ->
            viewModelScope.launch {
                recordAudioMessagePlayer.playAudio(
                    audioFile = audioFile
                )
            }
        }
    }

    fun onSliderPositionChange(position: Int) {
        viewModelScope.launch {
            recordAudioMessagePlayer.setPosition(
                position = position
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordAudioMessagePlayer.close()
    }
}
