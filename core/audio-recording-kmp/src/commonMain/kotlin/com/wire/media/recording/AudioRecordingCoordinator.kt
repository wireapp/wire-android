/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.media.recording

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.content.external.PlatformResult
import com.wire.content.media.MediaMetadataReader
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.media.player.AudioMediaPlayingState
import com.wire.media.player.AudioState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path
import okio.Path.Companion.toPath

@Suppress("TooManyFunctions")
class AudioRecordingCoordinator(
    private val audioRecorder: AudioRecorder,
    private val recordingPreview: RecordingPreview,
    private val effectsProcessor: AudioEffectsProcessor,
    private val metadataReader: MediaMetadataReader,
    private val fileSystem: KaliumFileSystem,
    private val loudnessBuilder: AudioNormalizedLoudnessBuilder,
    scope: CoroutineScope,
) {
    var state: AudioRecordingState by mutableStateOf(AudioRecordingState())
        private set

    private val mutableIssues = MutableSharedFlow<RecordingAvailability>(extraBufferCapacity = 1)
    val issues: SharedFlow<RecordingAvailability> = mutableIssues.asSharedFlow()

    private val operationMutex = Mutex()
    private var recordingFiles: AudioRecordingFiles? = null
    private var effectsPath: Path? = null

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recordingPreview.state.collect { previewState ->
                state = state.copy(audioState = previewState)
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            audioRecorder.events.collect(::handleRecorderEvent)
        }
    }

    suspend fun startRecording(maximumSizeBytes: Long): RecordingCapabilityResult<Unit> = operationMutex.withLock {
        if (state.buttonState != RecordingButtonState.ENABLED) {
            return@withLock RecordingCapabilityResult.Failure("invalid_recording_state")
        }
        when (val result = audioRecorder.start(maximumSizeBytes)) {
            is RecordingCapabilityResult.Success -> {
                recordingFiles = result.value
                effectsPath = result.value.originalWav.effectsPath()
                state = state.copy(
                    buttonState = RecordingButtonState.RECORDING,
                    originalOutputFile = result.value.originalWav,
                    effectsOutputFile = effectsPath.takeIf { state.shouldApplyEffects },
                    availability = RecordingAvailability.Available,
                )
                RecordingCapabilityResult.Success(Unit)
            }
            RecordingCapabilityResult.Unsupported -> unsupported(RecordingCapability.RECORDING)
            is RecordingCapabilityResult.Failure -> failed(RecordingCapability.RECORDING, result.reason)
        }
    }

    suspend fun stopRecording(): RecordingCapabilityResult<Unit> = operationMutex.withLock {
        stopRecordingLocked()
    }

    suspend fun setEffectsEnabled(enabled: Boolean): RecordingCapabilityResult<Unit> = operationMutex.withLock {
        state = state.copy(shouldApplyEffects = enabled)
        if (!enabled || state.buttonState != RecordingButtonState.READY_TO_SEND) {
            return@withLock RecordingCapabilityResult.Success(Unit)
        }

        val existingEffects = effectsPath?.takeIf(fileSystem::exists)
        if (existingEffects != null) {
            state = state.copy(effectsOutputFile = existingEffects)
            return@withLock RecordingCapabilityResult.Success(Unit)
        }

        state = state.copy(
            buttonState = RecordingButtonState.ENCODING,
            audioState = state.audioState.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching),
        )
        val result = processEffectsLocked()
        updateReadyState(playablePath())
        result
    }

    fun playablePath(): Path? = if (state.shouldApplyEffects) {
        state.effectsOutputFile
    } else {
        state.originalOutputFile
    }

    fun togglePreview(): RecordingCapabilityResult<Unit> {
        val path = playablePath() ?: return RecordingCapabilityResult.Failure("recording_unavailable")
        return recordingPreview.toggle(path).recordPreviewAvailability()
    }

    fun seekPreview(positionMs: Int): RecordingCapabilityResult<Unit> =
        recordingPreview.seekTo(positionMs).recordPreviewAvailability()

    fun stopPreview(): RecordingCapabilityResult<Unit> = recordingPreview.stop().recordPreviewAvailability()

    suspend fun discard(): RecordingAction = operationMutex.withLock {
        recordingPreview.stop()
        if (state.buttonState == RecordingButtonState.RECORDING) audioRecorder.stop() else audioRecorder.release()
        cleanupPaths()
        resetState()
        RecordingAction.Discarded
    }

    suspend fun finish(): RecordingCapabilityResult<RecordedAudio> = operationMutex.withLock {
        val source = playablePath() ?: return@withLock RecordingCapabilityResult.Failure("recording_unavailable")
        val destination = recordingFiles?.encodedM4a
            ?: return@withLock RecordingCapabilityResult.Failure("encoding_destination_unavailable")
        recordingPreview.stop()
        state = state.copy(buttonState = RecordingButtonState.ENCODING, audioState = AudioState.DEFAULT)

        val output = when (val encoding = audioRecorder.encode(source, destination)) {
            is RecordingCapabilityResult.Success -> encoding.value
            RecordingCapabilityResult.Unsupported -> {
                markUnsupported(RecordingCapability.ENCODING)
                deleteIfExists(destination)
                state = state.copy(buttonState = RecordingButtonState.READY_TO_SEND)
                return@withLock RecordingCapabilityResult.Unsupported
            }
            is RecordingCapabilityResult.Failure -> {
                markFailed(RecordingCapability.ENCODING, encoding.reason)
                deleteIfExists(destination)
                source
            }
        }
        val mimeType = if (output == destination) M4A_AUDIO_MIME_TYPE else WAV_AUDIO_MIME_TYPE
        val recorded = RecordedAudio(output, mimeType, state.wavesMask.orEmpty())
        cleanupPaths(except = output)
        val availability = state.availability
        resetState(availability)
        RecordingCapabilityResult.Success(recorded)
    }

    fun showDiscardDialog() {
        state = state.copy(discardDialogState = RecordingDialogState.Shown)
    }

    fun dismissDiscardDialog() {
        state = state.copy(discardDialogState = RecordingDialogState.Hidden)
    }

    fun showPermissionsDeniedDialog() {
        state = state.copy(permissionsDeniedDialogState = RecordingDialogState.Shown)
    }

    fun dismissPermissionsDeniedDialog() {
        state = state.copy(permissionsDeniedDialogState = RecordingDialogState.Hidden)
    }

    fun dismissMaxFileSizeDialog() {
        state = state.copy(maxFileSizeReachedDialogState = RecordingDialogState.Hidden)
    }

    fun release() {
        recordingPreview.release()
        audioRecorder.release()
        cleanupPaths()
        recordingFiles = null
        effectsPath = null
    }

    private suspend fun stopRecordingLocked(): RecordingCapabilityResult<Unit> {
        if (state.buttonState != RecordingButtonState.RECORDING) return RecordingCapabilityResult.Success(Unit)
        state = state.copy(
            buttonState = RecordingButtonState.ENCODING,
            audioState = state.audioState.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching),
        )
        val stopResult = audioRecorder.stop()
        when (stopResult) {
            RecordingCapabilityResult.Unsupported -> markUnsupported(RecordingCapability.RECORDING)
            is RecordingCapabilityResult.Failure -> markFailed(RecordingCapability.RECORDING, stopResult.reason)
            is RecordingCapabilityResult.Success -> Unit
        }
        if (state.shouldApplyEffects) processEffectsLocked()
        updateReadyState(playablePath())
        return stopResult
    }

    private suspend fun processEffectsLocked(): RecordingCapabilityResult<Unit> {
        val source = state.originalOutputFile ?: return RecordingCapabilityResult.Failure("recording_unavailable")
        val destination = effectsPath ?: source.effectsPath().also { effectsPath = it }
        return when (val result = effectsProcessor.process(AudioEffectsRequest(source, destination))) {
            is RecordingCapabilityResult.Success -> {
                state = state.copy(effectsOutputFile = result.value)
                RecordingCapabilityResult.Success(Unit)
            }
            RecordingCapabilityResult.Unsupported -> {
                deleteIfExists(destination)
                state = state.copy(shouldApplyEffects = false, effectsOutputFile = null)
                unsupported(RecordingCapability.EFFECTS)
            }
            is RecordingCapabilityResult.Failure -> {
                deleteIfExists(destination)
                state = state.copy(shouldApplyEffects = false, effectsOutputFile = null)
                failed(RecordingCapability.EFFECTS, result.reason)
            }
        }
    }

    private suspend fun updateReadyState(path: Path?) {
        val durationMs = path?.let { readDuration(it) } ?: 0
        val waves = path?.let { buildWaves(it) }.orEmpty()
        state = state.copy(
            buttonState = RecordingButtonState.READY_TO_SEND,
            audioState = AudioState.DEFAULT.copy(
                totalTimeInMs = AudioState.TotalTimeInMs.Known(durationMs),
            ),
            wavesMask = waves,
        )
    }

    private suspend fun readDuration(path: Path): Int =
        when (val result = metadataReader.read(path, WAV_AUDIO_MIME_TYPE)) {
            is PlatformResult.Success ->
                ((result.value as? AssetContent.AssetMetadata.Audio)?.durationMs ?: 0L).toInt()
            else -> 0
        }

    private suspend fun buildWaves(path: Path): List<Int> = try {
        loudnessBuilder(path.toString())?.map { it.toUByte().toInt() }.orEmpty()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }

    private suspend fun handleRecorderEvent(event: AudioRecorderEvent) {
        when (event) {
            is AudioRecorderEvent.MaxFileSizeReached -> {
                stopRecording()
                state = state.copy(
                    maxFileSizeReachedDialogState = RecordingDialogState.MaxFileSizeReached(
                        RecordingSizePolicy.megabytes(event.maximumSizeBytes)
                    )
                )
            }
            is AudioRecorderEvent.Interrupted -> {
                stopRecording()
            }
        }
    }

    private fun <T> unsupported(capability: RecordingCapability): RecordingCapabilityResult<T> {
        markUnsupported(capability)
        return RecordingCapabilityResult.Unsupported
    }

    private fun <T> failed(capability: RecordingCapability, reason: String?): RecordingCapabilityResult<T> {
        markFailed(capability, reason)
        return RecordingCapabilityResult.Failure(reason)
    }

    private fun markUnsupported(capability: RecordingCapability) {
        val issue = RecordingAvailability.Unsupported(capability)
        state = state.copy(availability = issue)
        mutableIssues.tryEmit(issue)
    }

    private fun markFailed(capability: RecordingCapability, reason: String?) {
        val issue = RecordingAvailability.Failed(capability, reason)
        state = state.copy(availability = issue)
        mutableIssues.tryEmit(issue)
    }

    private fun RecordingCapabilityResult<Unit>.recordPreviewAvailability(): RecordingCapabilityResult<Unit> = also {
        when (it) {
            RecordingCapabilityResult.Unsupported -> markUnsupported(RecordingCapability.PREVIEW)
            is RecordingCapabilityResult.Failure -> markFailed(RecordingCapability.PREVIEW, it.reason)
            is RecordingCapabilityResult.Success -> Unit
        }
    }

    private fun cleanupPaths(except: Path? = null) {
        val paths = buildSet {
            recordingFiles?.let {
                add(it.originalWav)
                add(it.encodedM4a)
            }
            effectsPath?.let(::add)
            state.originalOutputFile?.let(::add)
            state.effectsOutputFile?.let(::add)
        }
        paths.filterNot { it == except }.forEach(::deleteIfExists)
        recordingFiles = null
        effectsPath = null
    }

    private fun deleteIfExists(path: Path) {
        if (fileSystem.exists(path)) fileSystem.delete(path)
    }

    private fun resetState(availability: RecordingAvailability = RecordingAvailability.Available) {
        state = AudioRecordingState(
            shouldApplyEffects = state.shouldApplyEffects,
            availability = availability,
        )
    }

    private fun Path.effectsPath(): Path {
        val stem = name.substringBeforeLast('.', name)
        return parent?.resolve("$stem-filter.wav") ?: "$stem-filter.wav".toPath()
    }
}
