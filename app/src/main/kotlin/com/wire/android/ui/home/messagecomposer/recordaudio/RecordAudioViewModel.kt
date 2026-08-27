/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.messagecomposer.recordaudio

import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ui.UIText
import com.wire.content.media.MediaMetadataReader
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.media.recording.AudioEffectsProcessor
import com.wire.media.recording.AudioRecorder
import com.wire.media.recording.AudioRecordingCoordinator
import com.wire.media.recording.RecordingAction
import com.wire.media.recording.RecordingAvailability
import com.wire.media.recording.RecordingButtonState
import com.wire.media.recording.RecordingCapabilityResult
import com.wire.media.recording.RecordingPreview
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okio.Path

@Suppress("TooManyFunctions", "LongParameterList")
class RecordAudioViewModel @Inject constructor(
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val currentScreenManager: CurrentScreenManager,
    audioRecorder: AudioRecorder,
    recordingPreview: RecordingPreview,
    effectsProcessor: AudioEffectsProcessor,
    mediaMetadataReader: MediaMetadataReader,
    private val globalDataStore: GlobalDataStore,
    audioNormalizedLoudnessBuilder: AudioNormalizedLoudnessBuilder,
    kaliumFileSystem: KaliumFileSystem,
) : ActionsViewModel<RecordingAction>() {
    private val recordingCoordinator = AudioRecordingCoordinator(
        audioRecorder = audioRecorder,
        recordingPreview = recordingPreview,
        effectsProcessor = effectsProcessor,
        metadataReader = mediaMetadataReader,
        fileSystem = kaliumFileSystem,
        loudnessBuilder = audioNormalizedLoudnessBuilder,
        scope = viewModelScope,
    )

    val state: RecordAudioState
        get() = recordingCoordinator.state

    private var hasOngoingCall = false
    private val infoMessage = MutableSharedFlow<UIText>()

    init {
        observeEffectsCheckbox()
        observeScreenState()
        observeUserIsInCall()
        observeRecordingIssues()
    }

    fun getInfoMessage(): SharedFlow<UIText> = infoMessage.asSharedFlow()

    fun setApplyEffectsAndPlayAudio(enabled: Boolean) {
        val wasPlaying = state.audioState.isPlaying()
        viewModelScope.launch {
            setShouldApplyEffectsInternal(enabled)
            if (wasPlaying) recordingCoordinator.togglePreview()
        }
    }

    fun getPlayableAudioFile(): Path? = recordingCoordinator.playablePath()

    fun startRecording() {
        if (hasOngoingCall) {
            viewModelScope.launch {
                infoMessage.emit(RecordAudioInfoMessageType.UnableToRecordAudioCall.uiText)
            }
        } else {
            viewModelScope.launch {
                recordingCoordinator.startRecording(getAssetSizeLimit(false))
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            recordingCoordinator.stopRecording()
        }
    }

    fun showDiscardRecordingDialog() {
        if (state.buttonState == RecordingButtonState.ENABLED) {
            sendAction(RecordingAction.Discarded)
        } else {
            recordingCoordinator.showDiscardDialog()
        }
    }

    fun onDismissDiscardDialog() = recordingCoordinator.dismissDiscardDialog()

    fun showPermissionsDeniedDialog() = recordingCoordinator.showPermissionsDeniedDialog()

    fun onDismissPermissionsDeniedDialog() = recordingCoordinator.dismissPermissionsDeniedDialog()

    fun onDismissMaxFileSizeReachedDialog() = recordingCoordinator.dismissMaxFileSizeDialog()

    fun discardRecording() {
        viewModelScope.launch {
            sendAction(recordingCoordinator.discard())
        }
    }

    fun sendRecording() {
        viewModelScope.launch {
            when (val result = recordingCoordinator.finish()) {
                is RecordingCapabilityResult.Success -> sendAction(RecordingAction.Recorded(result.value))
                RecordingCapabilityResult.Unsupported,
                is RecordingCapabilityResult.Failure ->
                    infoMessage.emit(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText)
            }
        }
    }

    fun onPlayAudio() {
        recordingCoordinator.togglePreview()
    }

    fun onSliderPositionChange(position: Int) {
        recordingCoordinator.seekPreview(position)
    }

    fun setShouldApplyEffects(enabled: Boolean) {
        viewModelScope.launch {
            setShouldApplyEffectsInternal(enabled)
        }
    }

    override fun onCleared() {
        recordingCoordinator.release()
        super.onCleared()
    }

    private fun observeEffectsCheckbox() {
        viewModelScope.launch {
            globalDataStore.isRecordAudioEffectsCheckboxEnabled().collect { enabled ->
                recordingCoordinator.setEffectsEnabled(enabled)
            }
        }
    }

    private fun observeScreenState() {
        viewModelScope.launch {
            currentScreenManager.observeCurrentScreen(viewModelScope).collect { currentScreen ->
                if (
                    state.buttonState == RecordingButtonState.RECORDING &&
                    currentScreen == CurrentScreen.InBackground
                ) {
                    recordingCoordinator.stopRecording()
                }
            }
        }
    }

    private fun observeUserIsInCall() {
        viewModelScope.launch {
            observeEstablishedCalls().collect { calls ->
                hasOngoingCall = calls.isNotEmpty()
                if (hasOngoingCall && state.buttonState == RecordingButtonState.RECORDING) {
                    recordingCoordinator.stopRecording()
                }
            }
        }
    }

    private fun observeRecordingIssues() {
        viewModelScope.launch {
            recordingCoordinator.issues.collect { issue ->
                if (issue is RecordingAvailability.Failed || issue is RecordingAvailability.Unsupported) {
                    infoMessage.emit(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText)
                }
            }
        }
    }

    private suspend fun setShouldApplyEffectsInternal(enabled: Boolean) {
        globalDataStore.setRecordAudioEffectsCheckboxEnabled(enabled)
        recordingCoordinator.setEffectsEnabled(enabled)
    }
}
