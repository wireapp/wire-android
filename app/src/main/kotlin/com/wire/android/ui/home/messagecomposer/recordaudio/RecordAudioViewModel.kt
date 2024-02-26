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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.util.AUDIO_MIME_TYPE
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.getAudioLengthInMs
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
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

    fun getOutputFile(): File? = state.outputFile

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
            audioMediaRecorder.setUp()

            state = state.copy(
                outputFile = audioMediaRecorder.outputFile
            )

            audioMediaRecorder.startRecording()

            state = state.copy(
                buttonState = RecordAudioButtonState.RECORDING
            )
        }
    }

    fun stopRecording() {
        if (state.buttonState == RecordAudioButtonState.RECORDING) {
            audioMediaRecorder.stop()

            state = state.copy(
                buttonState = RecordAudioButtonState.READY_TO_SEND,
                audioState = AudioState.DEFAULT.copy(
                    totalTimeInMs = AudioState.TotalTimeInMs.Known(
                        state.outputFile?.let {
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
            state.outputFile?.toPath()?.deleteIfExists()
            recordAudioMessagePlayer.stop()
            recordAudioMessagePlayer.close()
            state = state.copy(
                buttonState = RecordAudioButtonState.ENABLED,
                discardDialogState = RecordAudioDialogState.Hidden,
                outputFile = null
            )
            onCloseRecordAudio()
        }
    }

    fun sendRecording(
        onAudioRecorded: (UriAsset) -> Unit,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            recordAudioMessagePlayer.stop()
            recordAudioMessagePlayer.close()
            state.outputFile?.let {
                onAudioRecorded(
                    UriAsset(
                        uri = it.toUri(),
                        saveToDeviceIfInvalid = false
                    )
                )
                onComplete()
                // TODO(RecordAudio): Question: Should we remove the file here as well?
            }
        }
    }

    fun onPlayAudio() {
        state.outputFile?.let { audioFile ->
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
