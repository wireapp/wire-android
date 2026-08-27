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

import com.wire.content.external.PlatformResult
import com.wire.content.media.MediaMetadataReader
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.media.player.AudioState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRecordingCoordinatorTest {
    @Test
    fun `recording with effects produces ready common state`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.effects.result = RecordingCapabilityResult.Success(arrangement.effectsPath)

        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)
        arrangement.coordinator.setEffectsEnabled(true)
        arrangement.coordinator.stopRecording()

        assertEquals(RecordingButtonState.READY_TO_SEND, arrangement.coordinator.state.buttonState)
        assertEquals(arrangement.effectsPath, arrangement.coordinator.playablePath())
        assertEquals(AudioState.TotalTimeInMs.Known(DURATION_MS), arrangement.coordinator.state.audioState.totalTimeInMs)
        assertEquals(listOf(1, 255), arrangement.coordinator.state.wavesMask)
        assertEquals(AudioEffectsRequest(arrangement.originalPath, arrangement.effectsPath), arrangement.effects.requests.single())
    }

    @Test
    fun `maximum size event stops recording and exposes size in megabytes`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)

        arrangement.recorder.mutableEvents.emit(AudioRecorderEvent.MaxFileSizeReached(MAXIMUM_SIZE_BYTES))
        runCurrent()

        assertEquals(1, arrangement.recorder.stopCount)
        assertEquals(
            RecordingDialogState.MaxFileSizeReached(5),
            arrangement.coordinator.state.maxFileSizeReachedDialogState,
        )
        assertEquals(RecordingButtonState.READY_TO_SEND, arrangement.coordinator.state.buttonState)
    }

    @Test
    fun `audio focus interruption stops an active recording`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)

        arrangement.recorder.mutableEvents.emit(AudioRecorderEvent.Interrupted(RecordingInterruption.AUDIO_FOCUS))
        runCurrent()

        assertEquals(1, arrangement.recorder.stopCount)
        assertEquals(RecordingButtonState.READY_TO_SEND, arrangement.coordinator.state.buttonState)
    }

    @Test
    fun `encoding failure keeps wav fallback and removes partial output`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.recorder.encodeResult = RecordingCapabilityResult.Failure("codec_failed")
        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)
        arrangement.coordinator.stopRecording()
        arrangement.fileSystem.write(arrangement.encodedPath)

        val result = arrangement.coordinator.finish()

        val recorded = assertIs<RecordingCapabilityResult.Success<RecordedAudio>>(result).value
        assertEquals(arrangement.originalPath, recorded.path)
        assertEquals(WAV_AUDIO_MIME_TYPE, recorded.mimeType)
        assertTrue(arrangement.fileSystem.exists(arrangement.originalPath))
        assertFalse(arrangement.fileSystem.exists(arrangement.encodedPath))
        assertIs<RecordingAvailability.Failed>(arrangement.coordinator.state.availability)
    }

    @Test
    fun `unsupported encoding stays explicit and keeps the recording ready`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.recorder.encodeResult = RecordingCapabilityResult.Unsupported
        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)
        arrangement.coordinator.stopRecording()
        arrangement.fileSystem.write(arrangement.encodedPath)

        val result = arrangement.coordinator.finish()

        assertEquals(RecordingCapabilityResult.Unsupported, result)
        assertTrue(arrangement.fileSystem.exists(arrangement.originalPath))
        assertFalse(arrangement.fileSystem.exists(arrangement.encodedPath))
        assertEquals(RecordingButtonState.READY_TO_SEND, arrangement.coordinator.state.buttonState)
        assertEquals(
            RecordingAvailability.Unsupported(RecordingCapability.ENCODING),
            arrangement.coordinator.state.availability,
        )
    }

    @Test
    fun `successful encoding keeps only encoded output`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.recorder.encodeResult = RecordingCapabilityResult.Success(arrangement.encodedPath)
        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)
        arrangement.coordinator.stopRecording()
        arrangement.fileSystem.write(arrangement.encodedPath)

        val result = arrangement.coordinator.finish()

        val recorded = assertIs<RecordingCapabilityResult.Success<RecordedAudio>>(result).value
        assertEquals(arrangement.encodedPath, recorded.path)
        assertEquals(M4A_AUDIO_MIME_TYPE, recorded.mimeType)
        assertFalse(arrangement.fileSystem.exists(arrangement.originalPath))
        assertTrue(arrangement.fileSystem.exists(arrangement.encodedPath))
    }

    @Test
    fun `discard stops recording and removes every session file`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)
        arrangement.fileSystem.write(arrangement.effectsPath)
        arrangement.fileSystem.write(arrangement.encodedPath)

        val action = arrangement.coordinator.discard()

        assertEquals(RecordingAction.Discarded, action)
        assertEquals(1, arrangement.recorder.stopCount)
        assertFalse(arrangement.fileSystem.exists(arrangement.originalPath))
        assertFalse(arrangement.fileSystem.exists(arrangement.effectsPath))
        assertFalse(arrangement.fileSystem.exists(arrangement.encodedPath))
    }

    @Test
    fun `unsupported recorder remains explicit in common state`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.recorder.startResult = RecordingCapabilityResult.Unsupported

        val result = arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)

        assertEquals(RecordingCapabilityResult.Unsupported, result)
        assertEquals(
            RecordingAvailability.Unsupported(RecordingCapability.RECORDING),
            arrangement.coordinator.state.availability,
        )
        assertEquals(RecordingButtonState.ENABLED, arrangement.coordinator.state.buttonState)
    }

    @Test
    fun `effects failure falls back to original recording`() = runTest {
        val arrangement = Arrangement(backgroundScope)
        arrangement.effects.result = RecordingCapabilityResult.Failure("effect_failed")
        arrangement.coordinator.setEffectsEnabled(true)
        arrangement.coordinator.startRecording(MAXIMUM_SIZE_BYTES)

        arrangement.coordinator.stopRecording()

        assertEquals(arrangement.originalPath, arrangement.coordinator.playablePath())
        assertFalse(arrangement.coordinator.state.shouldApplyEffects)
        assertFalse(arrangement.fileSystem.exists(arrangement.effectsPath))
        assertIs<RecordingAvailability.Failed>(arrangement.coordinator.state.availability)
    }

    @Test
    fun `size policy uses byte limit without multiplying it again`() {
        assertFalse(RecordingSizePolicy.hasReachedLimit(MAXIMUM_SIZE_BYTES, MAXIMUM_SIZE_BYTES))
        assertTrue(RecordingSizePolicy.hasReachedLimit(MAXIMUM_SIZE_BYTES + 1, MAXIMUM_SIZE_BYTES))
        assertEquals(5, RecordingSizePolicy.megabytes(MAXIMUM_SIZE_BYTES))
    }

    private class Arrangement(scope: CoroutineScope) {
        val fileSystem = TestKaliumFileSystem()
        val originalPath = "/cache/wire-audio.wav".toPath()
        val encodedPath = "/cache/wire-audio.m4a".toPath()
        val effectsPath = "/cache/wire-audio-filter.wav".toPath()
        val recorder = FakeAudioRecorder(fileSystem, AudioRecordingFiles(originalPath, encodedPath))
        val preview = FakeRecordingPreview()
        val effects = FakeEffectsProcessor(fileSystem)
        val coordinator = AudioRecordingCoordinator(
            audioRecorder = recorder,
            recordingPreview = preview,
            effectsProcessor = effects,
            metadataReader = MediaMetadataReader { _, _ ->
                PlatformResult.Success(AssetContent.AssetMetadata.Audio(DURATION_MS.toLong(), null))
            },
            fileSystem = fileSystem,
            loudnessBuilder = AudioNormalizedLoudnessBuilder { byteArrayOf(1, -1) },
            scope = scope,
        )
    }

    private class FakeAudioRecorder(
        private val fileSystem: TestKaliumFileSystem,
        private val files: AudioRecordingFiles,
    ) : AudioRecorder {
        val mutableEvents = MutableSharedFlow<AudioRecorderEvent>(extraBufferCapacity = 1)
        override val events: Flow<AudioRecorderEvent> = mutableEvents
        var startResult: RecordingCapabilityResult<AudioRecordingFiles> = RecordingCapabilityResult.Success(files)
        var encodeResult: RecordingCapabilityResult<Path> = RecordingCapabilityResult.Success(files.encodedM4a)
        var stopCount = 0

        override suspend fun start(maximumSizeBytes: Long): RecordingCapabilityResult<AudioRecordingFiles> {
            if (startResult is RecordingCapabilityResult.Success) fileSystem.write(files.originalWav)
            return startResult
        }

        override suspend fun stop(): RecordingCapabilityResult<Unit> {
            stopCount++
            return RecordingCapabilityResult.Success(Unit)
        }

        override suspend fun encode(source: Path, destination: Path): RecordingCapabilityResult<Path> = encodeResult

        override fun release() = Unit
    }

    private class FakeRecordingPreview : RecordingPreview {
        override val state: StateFlow<AudioState> = MutableStateFlow(AudioState.DEFAULT)
        override fun toggle(path: Path): RecordingCapabilityResult<Unit> = RecordingCapabilityResult.Success(Unit)
        override fun seekTo(positionMs: Int): RecordingCapabilityResult<Unit> = RecordingCapabilityResult.Success(Unit)
        override fun stop(): RecordingCapabilityResult<Unit> = RecordingCapabilityResult.Success(Unit)
        override fun release() = Unit
    }

    private class FakeEffectsProcessor(
        private val fileSystem: TestKaliumFileSystem,
    ) : AudioEffectsProcessor {
        val requests = mutableListOf<AudioEffectsRequest>()
        var result: RecordingCapabilityResult<Path> = RecordingCapabilityResult.Failure()

        override suspend fun process(request: AudioEffectsRequest): RecordingCapabilityResult<Path> {
            requests += request
            if (result is RecordingCapabilityResult.Success) fileSystem.write(request.destination)
            return result
        }
    }

    private class TestKaliumFileSystem : KaliumFileSystem {
        private val delegate = FakeFileSystem()
        override val rootCachePath: Path = "/cache".toPath()
        override val rootDBPath: Path = "/db".toPath()

        init {
            delegate.createDirectories(rootCachePath)
            delegate.createDirectories(rootDBPath)
        }

        fun write(path: Path) {
            delegate.sink(path).buffer().use { it.writeUtf8("audio") }
        }

        override fun sink(outputPath: Path, mustCreate: Boolean): Sink = delegate.sink(outputPath, mustCreate)
        override fun source(inputPath: Path): Source = delegate.source(inputPath)
        override fun createDirectories(dir: Path) = delegate.createDirectories(dir)
        override fun createDirectory(dir: Path, mustCreate: Boolean) = delegate.createDirectory(dir, mustCreate)
        override fun delete(path: Path, mustExist: Boolean) = delegate.delete(path, mustExist)
        override fun deleteContents(dir: Path, mustExist: Boolean) {
            delegate.deleteRecursively(dir, mustExist)
            delegate.createDirectories(dir)
        }
        override fun exists(path: Path): Boolean = delegate.exists(path)
        override fun copy(sourcePath: Path, targetPath: Path) {
            delegate.source(sourcePath).use { source ->
                delegate.sink(targetPath).buffer().use { sink -> sink.writeAll(source) }
            }
        }
        override fun tempFilePath(pathString: String?): Path = rootCachePath.resolve(pathString ?: "temp")
        override fun providePersistentAssetPath(assetName: String): Path = rootCachePath.resolve(assetName)
        override fun selfUserAvatarPath(): Path = rootCachePath.resolve("avatar")
        override suspend fun readByteArray(inputPath: Path): ByteArray = source(inputPath).buffer().use { it.readByteArray() }
        override suspend fun writeData(outputSink: Sink, dataSource: Source): Long =
            outputSink.buffer().use { it.writeAll(dataSource) }
        override suspend fun listDirectories(dir: Path): List<Path> = delegate.list(dir)
        override fun size(path: Path): Long? = delegate.metadataOrNull(path)?.size
    }

    private companion object {
        const val DURATION_MS = 4_321
        const val MAXIMUM_SIZE_BYTES = 5L * 1_024L * 1_024L
    }
}
