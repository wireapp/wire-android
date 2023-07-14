/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.io.path.deleteIfExists

@Suppress("TooManyFunctions")
interface RecordAudioViewModel {
    fun getButtonState(): RecordAudioButtonState
    fun getDiscardDialogState(): RecordAudioDialogState
    fun getPermissionsDeniedDialogState(): RecordAudioDialogState
    fun getMaxFileSizeReachedDialogState(): RecordAudioDialogState
    fun getInfoMessage(): SharedFlow<UIText>
    fun getOutputFile(): File?
    fun getAudioState(): AudioState
    fun startRecording()
    fun stopRecording()
    fun showDiscardRecordingDialog(onCloseRecordAudio: () -> Unit)
    fun onDismissDiscardDialog()
    fun showPermissionsDeniedDialog()
    fun onDismissPermissionsDeniedDialog()
    fun onDismissMaxFileSizeReachedDialog()
    fun discardRecording(onCloseRecordAudio: () -> Unit)
    fun sendRecording(onAudioRecorded: (UriAsset) -> Unit, onComplete: () -> Unit)
    fun onPlayAudio()
    fun onSliderPositionChange(position: Int)
}

@Suppress("TooManyFunctions")
@HiltViewModel
class RecordAudioViewModelImpl @Inject constructor(
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val audioMediaRecorder: AudioMediaRecorder
) : RecordAudioViewModel, ViewModel() {

    private var state: RecordAudioState by mutableStateOf(RecordAudioState())

    private var hasOngoingCall: Boolean = false

    private val infoMessage = MutableSharedFlow<UIText>()

    override fun getButtonState(): RecordAudioButtonState = state.buttonState

    override fun getDiscardDialogState(): RecordAudioDialogState = state.discardDialogState

    override fun getPermissionsDeniedDialogState(): RecordAudioDialogState =
        state.permissionsDeniedDialogState

    override fun getMaxFileSizeReachedDialogState(): RecordAudioDialogState =
        state.maxFileSizeReachedDialogState

    override fun getOutputFile(): File? = state.outputFile

    override fun getAudioState(): AudioState = state.audioState

    override fun getInfoMessage(): SharedFlow<UIText> = infoMessage.asSharedFlow()

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

    override fun startRecording() {
        if (hasOngoingCall) {
            viewModelScope.launch {
                infoMessage.emit(RecordAudioInfoMessageType.UnableToRecordAudioCall.uiText)
            }
        } else {
            state = state.copy(
                buttonState = RecordAudioButtonState.RECORDING
            )

            audioMediaRecorder.setUp()

            state = state.copy(
                outputFile = audioMediaRecorder.outputFile
            )

            audioMediaRecorder.startRecording()
        }
    }

    override fun stopRecording() {
        if (state.buttonState == RecordAudioButtonState.RECORDING) {
            state = state.copy(
                buttonState = RecordAudioButtonState.READY_TO_SEND
            )
            audioMediaRecorder.stop()
        }
        audioMediaRecorder.release()
    }

    override fun showDiscardRecordingDialog(onCloseRecordAudio: () -> Unit) {
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

    override fun onDismissDiscardDialog() {
        state = state.copy(
            discardDialogState = RecordAudioDialogState.Hidden
        )
    }

    override fun showPermissionsDeniedDialog() {
        state = state.copy(
            permissionsDeniedDialogState = RecordAudioDialogState.Shown
        )
    }

    override fun onDismissPermissionsDeniedDialog() {
        state = state.copy(
            permissionsDeniedDialogState = RecordAudioDialogState.Hidden
        )
    }

    override fun onDismissMaxFileSizeReachedDialog() {
        state = state.copy(
            maxFileSizeReachedDialogState = RecordAudioDialogState.Hidden
        )
    }

    override fun discardRecording(onCloseRecordAudio: () -> Unit) {
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

    override fun sendRecording(
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

    override fun onPlayAudio() {
        state.outputFile?.let { audioFile ->
            viewModelScope.launch {
                recordAudioMessagePlayer.playAudio(
                    audioFile = audioFile
                )
            }
        }
    }

    override fun onSliderPositionChange(position: Int) {
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
