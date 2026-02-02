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
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.media.audiomessage.AudioFocusHelper
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.media.audiomessage.toWavesMask
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.SUPPORTED_AUDIO_MIME_TYPE
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.fileDateTime
import com.wire.android.util.fromNioPathToContentUri
import com.wire.android.util.getAudioLengthInMs
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.util.DateTimeUtil
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

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class RecordAudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val generateAudioFileWithEffects: GenerateAudioFileWithEffectsUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val audioMediaRecorder: AudioMediaRecorder,
    private val globalDataStore: GlobalDataStore,
    private val audioNormalizedLoudnessBuilder: AudioNormalizedLoudnessBuilder,
    private val audioFocusHelper: AudioFocusHelper,
    private val dispatchers: DispatcherProvider,
    private val kaliumFileSystem: KaliumFileSystem
) : ActionsViewModel<RecordAudioViewActions>() {

    var state: RecordAudioState by mutableStateOf(RecordAudioState())
        private set

    private var hasOngoingCall: Boolean = false

    private val infoMessage = MutableSharedFlow<UIText>()

    private val tag = "RecordAudioViewModel"

    fun getInfoMessage(): SharedFlow<UIText> = infoMessage.asSharedFlow()

    fun setApplyEffectsAndPlayAudio(enabled: Boolean) {
        setShouldApplyEffects(enabled = enabled)
        if (state.audioState.isPlaying()) {
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
            audioFocusHelper.requestExclusive()
            viewModelScope.launch(dispatchers.default()) {
                val assetSizeLimit = getAssetSizeLimit(false)
                if (state.shouldApplyEffects && state.effectsOutputFile == null) {
                    state = state.copy(
                        effectsOutputFile = kaliumFileSystem
                            .tempFilePath(getRecordingAudioEffectsFileName()).toFile()
                    )
                }
                audioMediaRecorder.setUp(assetSizeLimit)
                if (audioMediaRecorder.startRecording()) {
                    state = state.copy(
                        originalOutputFile = audioMediaRecorder.originalOutputPath!!.toFile(),
                        buttonState = RecordAudioButtonState.RECORDING
                    )
                } else {
                    infoMessage.emit(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText)
                }
            }
        }
    }

    fun stopRecording() {
        audioFocusHelper.abandonExclusive()
        viewModelScope.launch(dispatchers.default()) {
            if (state.buttonState == RecordAudioButtonState.RECORDING) {
                appLogger.i("[$tag] -> Stopping audioMediaRecorder")
                audioMediaRecorder.stop()
            }
            appLogger.i("[$tag] -> Releasing audioMediaRecorder")
            audioMediaRecorder.release()

            if (state.originalOutputFile != null) {
                state = state.copy(
                    buttonState = RecordAudioButtonState.ENCODING,
                    audioState = state.audioState.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching)
                )
                if (state.shouldApplyEffects && state.effectsOutputFile != null) {
                    generateAudioFileWithEffects(
                        context = context,
                        originalFilePath = state.originalOutputFile!!.path,
                        effectsFilePath = state.effectsOutputFile!!.path
                    )
                }

                val playableAudioFile = getPlayableAudioFile()
                state = state.copy(
                    buttonState = RecordAudioButtonState.READY_TO_SEND,
                    audioState = AudioState.DEFAULT.copy(
                        totalTimeInMs = AudioState.TotalTimeInMs.Known(
                            playableAudioFile?.let {
                                getAudioLengthInMs(
                                    dataPath = it.path.toPath(),
                                    mimeType = SUPPORTED_AUDIO_MIME_TYPE
                                ).toInt()
                            } ?: 0
                        ),
                    ),
                    wavesMask = playableAudioFile?.let { audioNormalizedLoudnessBuilder(it.path) }?.toWavesMask() ?: listOf()
                )
            }
        }
    }

    fun showDiscardRecordingDialog() {
        when (state.buttonState) {
            RecordAudioButtonState.ENABLED -> sendAction(RecordAudioViewActions.Discarded)
            RecordAudioButtonState.RECORDING,
            RecordAudioButtonState.READY_TO_SEND,
            RecordAudioButtonState.ENCODING -> {
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

    fun discardRecording() {
        viewModelScope.launch {
            state.originalOutputFile?.toPath()?.deleteIfExists()
            state.effectsOutputFile?.toPath()?.deleteIfExists()
            recordAudioMessagePlayer.stop()
            state = state.copy(
                buttonState = RecordAudioButtonState.ENABLED,
                discardDialogState = RecordAudioDialogState.Hidden,
                originalOutputFile = null,
                effectsOutputFile = null
            )
            sendAction(RecordAudioViewActions.Discarded)
        }
    }

    fun sendRecording() {
        viewModelScope.launch {
            recordAudioMessagePlayer.stop()

            val outputFile = state.originalOutputFile
            val effectsFile = state.effectsOutputFile
            val wavesMask = state.wavesMask
            state = state.copy(
                buttonState = RecordAudioButtonState.ENCODING, audioState = AudioState.DEFAULT,
                originalOutputFile = null,
                effectsOutputFile = null
            )

            val didSucceed = if (state.shouldApplyEffects) {
                audioMediaRecorder.convertWavToM4a(effectsFile!!.toString())
            } else {
                audioMediaRecorder.convertWavToM4a(outputFile!!.toString())
            }

            try {
                when {
                    didSucceed -> {
                        outputFile?.toPath()?.deleteIfExists()
                        effectsFile?.toPath()?.deleteIfExists()
                    }

                    state.shouldApplyEffects -> {
                        outputFile?.toPath()?.deleteIfExists()
                    }

                    !state.shouldApplyEffects -> {
                        effectsFile?.toPath()?.deleteIfExists()
                    }
                }
            } catch (exception: IOException) {
                appLogger.e("[$tag] -> Couldn't delete audio files")
            }
            sendAction(
                RecordAudioViewActions.Recorded(
                    uriAsset = UriAsset(
                        uri = if (didSucceed) {
                            context.fromNioPathToContentUri(nioPath = audioMediaRecorder.m4aOutputPath!!.toNioPath())
                        } else {
                            if (state.shouldApplyEffects) {
                                context.fromNioPathToContentUri(nioPath = state.effectsOutputFile!!.toPath())
                            } else {
                                context.fromNioPathToContentUri(nioPath = state.originalOutputFile!!.toPath())
                            }
                        },
                        mimeType = if (didSucceed) {
                            "audio/mp4"
                        } else {
                            "audio/wav"
                        },
                        saveToDeviceIfInvalid = false,
                        audioWavesMask = wavesMask
                    )
                )
            )
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
            if (enabled && state.effectsOutputFile == null) {
                val effectsFile = kaliumFileSystem
                    .tempFilePath(getRecordingAudioEffectsFileName()).toFile()
                if (state.buttonState == RecordAudioButtonState.READY_TO_SEND) {
                    state = state.copy(
                        buttonState = RecordAudioButtonState.ENCODING,
                        audioState = state.audioState.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching)
                    )

                    generateAudioFileWithEffects(
                        context = context,
                        originalFilePath = state.originalOutputFile!!.path,
                        effectsFilePath = effectsFile.path
                    )

                    state = state.copy(
                        effectsOutputFile = effectsFile,
                        buttonState = RecordAudioButtonState.READY_TO_SEND,
                        audioState = AudioState(
                            audioMediaPlayingState = AudioMediaPlayingState.Stopped,
                            currentPositionInMs = 0,
                            AudioState.TotalTimeInMs.Known(
                                getAudioLengthInMs(
                                    dataPath = effectsFile.path.toPath(),
                                    mimeType = SUPPORTED_AUDIO_MIME_TYPE
                                ).toInt()
                            ),
                        ),
                        wavesMask = state.wavesMask,
                        shouldApplyEffects = true
                    )
                } else {
                    state = state.copy(
                        effectsOutputFile = effectsFile,
                        shouldApplyEffects = true
                    )
                }
            } else {
                state = state.copy(
                    shouldApplyEffects = enabled
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordAudioMessagePlayer.close()
    }

    companion object {
        fun getRecordingAudioEffectsFileName(): String = "wire-audio-${DateTimeUtil.currentInstant().fileDateTime()}-filter.wav"
    }
}

sealed interface RecordAudioViewActions {
    data object Discarded : RecordAudioViewActions
    data class Recorded(val uriAsset: UriAsset) : RecordAudioViewActions
}
