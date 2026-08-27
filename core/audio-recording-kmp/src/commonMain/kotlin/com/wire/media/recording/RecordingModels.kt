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

import com.wire.media.player.AudioState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import okio.Path

data class AudioRecordingState(
    val buttonState: RecordingButtonState = RecordingButtonState.ENABLED,
    val discardDialogState: RecordingDialogState = RecordingDialogState.Hidden,
    val permissionsDeniedDialogState: RecordingDialogState = RecordingDialogState.Hidden,
    val maxFileSizeReachedDialogState: RecordingDialogState = RecordingDialogState.Hidden,
    val originalOutputFile: Path? = null,
    val effectsOutputFile: Path? = null,
    val shouldApplyEffects: Boolean = false,
    val audioState: AudioState = AudioState.DEFAULT,
    val wavesMask: List<Int>? = null,
    val availability: RecordingAvailability = RecordingAvailability.Available,
)

enum class RecordingButtonState {
    ENABLED,
    RECORDING,
    READY_TO_SEND,
    ENCODING,
}

sealed interface RecordingDialogState {
    data object Shown : RecordingDialogState
    data object Hidden : RecordingDialogState
    data class MaxFileSizeReached(val maxSize: Long) : RecordingDialogState
}

enum class RecordingCapability {
    RECORDING,
    PREVIEW,
    EFFECTS,
    ENCODING,
}

sealed interface RecordingAvailability {
    data object Available : RecordingAvailability
    data class Unsupported(val capability: RecordingCapability) : RecordingAvailability
    data class Failed(val capability: RecordingCapability, val reason: String? = null) : RecordingAvailability
}

data class AudioRecordingFiles(
    val originalWav: Path,
    val encodedM4a: Path,
)

data class RecordedAudio(
    val path: Path,
    val mimeType: String,
    val audioWavesMask: List<Int>,
)

sealed interface RecordingAction {
    data object Discarded : RecordingAction
    data class Recorded(val recording: RecordedAudio) : RecordingAction
}

sealed interface RecordingCapabilityResult<out T> {
    data class Success<T>(val value: T) : RecordingCapabilityResult<T>
    data object Unsupported : RecordingCapabilityResult<Nothing>
    data class Failure(val reason: String? = null) : RecordingCapabilityResult<Nothing>
}

sealed interface AudioRecorderEvent {
    data class MaxFileSizeReached(val maximumSizeBytes: Long) : AudioRecorderEvent
    data class Interrupted(val reason: RecordingInterruption) : AudioRecorderEvent
}

enum class RecordingInterruption {
    AUDIO_FOCUS,
}

interface AudioRecorder {
    val events: Flow<AudioRecorderEvent>

    suspend fun start(maximumSizeBytes: Long): RecordingCapabilityResult<AudioRecordingFiles>
    suspend fun stop(): RecordingCapabilityResult<Unit>
    suspend fun encode(source: Path, destination: Path): RecordingCapabilityResult<Path>
    fun release()
}

interface RecordingPreview {
    val state: StateFlow<AudioState>

    fun toggle(path: Path): RecordingCapabilityResult<Unit>
    fun seekTo(positionMs: Int): RecordingCapabilityResult<Unit>
    fun stop(): RecordingCapabilityResult<Unit>
    fun release()
}

data class AudioEffectsRequest(
    val source: Path,
    val destination: Path,
)

fun interface AudioEffectsProcessor {
    suspend fun process(request: AudioEffectsRequest): RecordingCapabilityResult<Path>
}

object UnsupportedAudioRecorder : AudioRecorder {
    override val events: Flow<AudioRecorderEvent> = emptyFlow()

    override suspend fun start(maximumSizeBytes: Long): RecordingCapabilityResult<AudioRecordingFiles> =
        RecordingCapabilityResult.Unsupported

    override suspend fun stop(): RecordingCapabilityResult<Unit> = RecordingCapabilityResult.Unsupported

    override suspend fun encode(source: Path, destination: Path): RecordingCapabilityResult<Path> =
        RecordingCapabilityResult.Unsupported

    override fun release() = Unit
}

object UnsupportedRecordingPreview : RecordingPreview {
    override val state: StateFlow<AudioState> = MutableStateFlow(AudioState.DEFAULT)

    override fun toggle(path: Path): RecordingCapabilityResult<Unit> = RecordingCapabilityResult.Unsupported
    override fun seekTo(positionMs: Int): RecordingCapabilityResult<Unit> = RecordingCapabilityResult.Unsupported
    override fun stop(): RecordingCapabilityResult<Unit> = RecordingCapabilityResult.Unsupported
    override fun release() = Unit
}

object UnsupportedAudioEffectsProcessor : AudioEffectsProcessor {
    override suspend fun process(request: AudioEffectsRequest): RecordingCapabilityResult<Path> =
        RecordingCapabilityResult.Unsupported
}

object RecordingSizePolicy {
    const val BYTES_PER_MEGABYTE: Long = 1_024L * 1_024L

    fun hasReachedLimit(currentSizeBytes: Long, maximumSizeBytes: Long): Boolean =
        currentSizeBytes > maximumSizeBytes

    fun megabytes(maximumSizeBytes: Long): Long = maximumSizeBytes / BYTES_PER_MEGABYTE
}

const val WAV_AUDIO_MIME_TYPE: String = "audio/wav"
const val M4A_AUDIO_MIME_TYPE: String = "audio/mp4"
