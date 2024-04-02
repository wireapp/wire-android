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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.media.audiomessage.AudioMediaPlayingState
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
import java.io.IOException
import javax.inject.Inject
import kotlin.io.path.deleteIfExists

@Suppress("TooManyFunctions")
@HiltViewModel
class RecordAudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val generateAudioFileWithEffects: GenerateAudioFileWithEffectsUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val audioMediaRecorder: AudioMediaRecorder,
    private val globalDataStore: GlobalDataStore
) : ViewModel() {

    var state: RecordAudioState by mutableStateOf(RecordAudioState())
        private set

    private var hasOngoingCall: Boolean = false

    private val infoMessage = MutableSharedFlow<UIText>()

    private val tag = "RecordAudioViewModel"

    fun getInfoMessage(): SharedFlow<UIText> = infoMessage.asSharedFlow()

    fun setApplyEffectsAndPlayAudio(enabled: Boolean) {
        setShouldApplyEffects(enabled = enabled)
        if (state.audioState.audioMediaPlayingState is AudioMediaPlayingState.Playing) {
            onPlayAudio()
        }
    }

    fun getPlayableAudioFile(): File? = if (state.shouldApplyEffects) {
        state.effectsOutputFile
    } else {
        state.originalOutputFile
    }

    init {
        observeAudioPlayerState()
        observeEffectsCheckbox()

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

    private fun observeEffectsCheckbox() {
        viewModelScope.launch {
            globalDataStore.isRecordAudioEffectsCheckboxEnabled().collect {
                state = state.copy(shouldApplyEffects = it)
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
        }
        audioMediaRecorder.release()

        if (state.originalOutputFile != null && state.effectsOutputFile != null) {
            generateAudioFileWithEffects(
                context = context,
                originalFilePath = state.originalOutputFile!!.path,
                effectsFilePath = state.effectsOutputFile!!.path
            )

            state = state.copy(
                buttonState = RecordAudioButtonState.READY_TO_SEND,
                audioState = AudioState.DEFAULT.copy(
                    totalTimeInMs = AudioState.TotalTimeInMs.Known(
                        getPlayableAudioFile()?.let {
                            getAudioLengthInMs(
                                dataPath = it.path.toPath(),
                                mimeType = AUDIO_MIME_TYPE
                            ).toInt()
                        } ?: 0
                    )
                )
            )
        }
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
            state.effectsOutputFile?.toPath()?.deleteIfExists()
            recordAudioMessagePlayer.stop()
            recordAudioMessagePlayer.close()
            state = state.copy(
                buttonState = RecordAudioButtonState.ENABLED,
                discardDialogState = RecordAudioDialogState.Hidden,
                originalOutputFile = null,
                effectsOutputFile = null
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

            val resultFile = if (state.shouldApplyEffects) {
                try {
                    state.originalOutputFile?.toPath()?.deleteIfExists()
                } catch (exception: IOException) {
                    appLogger.e("[$tag] -> Couldn't delete original audio file before sending audio file with effects.")
                }
                state.effectsOutputFile!!.toUri()
            } else {
                try {
                    state.effectsOutputFile?.toPath()?.deleteIfExists()
                } catch (exception: IOException) {
                    appLogger.e("[$tag] -> Couldn't delete audio file with effects before sending original audio file.")
                }
                state.originalOutputFile!!.toUri()
            }

            onAudioRecorded(
                UriAsset(
                    uri = resultFile,
                    saveToDeviceIfInvalid = false
                )
            )
            onComplete()
        }
    }

    fun onPlayAudio() {
        getPlayableAudioFile()?.let { audioFile ->
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

    fun setShouldApplyEffects(enabled: Boolean) {
        viewModelScope.launch {
            globalDataStore.setRecordAudioEffectsCheckboxEnabled(enabled)
        }
        state = state.copy(
            shouldApplyEffects = enabled
        )
    }

    override fun onCleared() {
        super.onCleared()
        recordAudioMessagePlayer.close()
    }
}
