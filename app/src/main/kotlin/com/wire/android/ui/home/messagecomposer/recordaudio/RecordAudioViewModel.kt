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

import com.wire.android.di.ApplicationContext
import dev.zacsweers.metro.Inject

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import java.io.File
import java.io.IOException
import kotlin.io.path.deleteIfExists

@Suppress("TooManyFunctions", "LongParameterList")
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

    // stopRecording can be triggered several times at once (e.g. ON_STOP and going to background),
    // so stopping and releasing the recorder must not run concurrently
    private val stopRecorderMutex = Mutex()

    // the work after the recorder stops (encoding, length, waves); discard and a new recording cancel it
    private var encodingJob: Job? = null

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
            encodingJob?.cancel()
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
        encodingJob?.cancel()
        encodingJob = viewModelScope.launch(dispatchers.default()) {
            stopAndReleaseRecorder()

            val originalFile = state.originalOutputFile ?: return@launch
            val effectsFile = state.effectsOutputFile
            val applyEffects = state.shouldApplyEffects && effectsFile != null
            state = state.copy(
                buttonState = RecordAudioButtonState.ENCODING,
                audioState = state.audioState.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching)
            )
            if (applyEffects) {
                generateAudioFileWithEffects(
                    context = context,
                    originalFilePath = originalFile.path,
                    effectsFilePath = effectsFile!!.path
                )
            }

            val playableAudioFile = if (state.shouldApplyEffects) effectsFile else originalFile
            val totalTimeInMs = playableAudioFile?.let {
                getAudioLengthInMs(dataPath = it.path.toPath(), mimeType = SUPPORTED_AUDIO_MIME_TYPE).toInt()
            } ?: 0
            val wavesMask = playableAudioFile?.let { audioNormalizedLoudnessBuilder(it.path) }?.toWavesMask() ?: listOf()
            // a discard or a new recording cancels this job, and must not be overwritten by it
            ensureActive()
            state = state.copy(
                buttonState = RecordAudioButtonState.READY_TO_SEND,
                audioState = AudioState.DEFAULT.copy(totalTimeInMs = AudioState.TotalTimeInMs.Known(totalTimeInMs)),
                wavesMask = wavesMask
            )
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

    // stop() is a no-op when nothing is recording. Releasing without it leaves the writer loop running,
    // and that loop then reads from the next recording's AudioRecord.
    private suspend fun stopAndReleaseRecorder() = withContext(dispatchers.default()) {
        stopRecorderMutex.withLock {
            appLogger.i("[$tag] -> Stopping audioMediaRecorder")
            audioMediaRecorder.stop()
            appLogger.i("[$tag] -> Releasing audioMediaRecorder")
            audioMediaRecorder.release()
        }
    }

    fun discardRecording() {
        encodingJob?.cancel()
        viewModelScope.launch {
            stopAndReleaseRecorder()
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
            val inputFile = (if (state.shouldApplyEffects) effectsFile else outputFile) ?: return@launch
            val wavesMask = state.wavesMask
            state = state.copy(
                buttonState = RecordAudioButtonState.ENCODING, audioState = AudioState.DEFAULT,
                originalOutputFile = null,
                effectsOutputFile = null
            )

            val didSucceed = audioMediaRecorder.convertWavToM4a(inputFile.path)

            try {
                if (didSucceed || outputFile != inputFile) {
                    outputFile?.toPath()?.deleteIfExists()
                }
                if (didSucceed || effectsFile != inputFile) {
                    effectsFile?.toPath()?.deleteIfExists()
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
                            context.fromNioPathToContentUri(nioPath = inputFile.toPath())
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
            // the ViewModel outlives the record panel, so the next open must not start in ENCODING
            state = state.copy(buttonState = RecordAudioButtonState.ENABLED, audioState = AudioState.DEFAULT, wavesMask = null)
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
