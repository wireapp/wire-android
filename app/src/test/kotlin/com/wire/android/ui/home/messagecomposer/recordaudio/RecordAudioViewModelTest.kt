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

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.content.external.PlatformResult
import com.wire.content.media.MediaMetadataReader
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.media.player.AudioState
import com.wire.media.recording.AudioEffectsProcessor
import com.wire.media.recording.AudioRecorder
import com.wire.media.recording.AudioRecorderEvent
import com.wire.media.recording.AudioRecordingFiles
import com.wire.media.recording.M4A_AUDIO_MIME_TYPE
import com.wire.media.recording.RecordingAction
import com.wire.media.recording.RecordingAvailability
import com.wire.media.recording.RecordingButtonState
import com.wire.media.recording.RecordingCapability
import com.wire.media.recording.RecordingCapabilityResult
import com.wire.media.recording.RecordingDialogState
import com.wire.media.recording.RecordingPreview
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class RecordAudioViewModelTest {
    @Test
    fun `ongoing call prevents recording and shows call message`() = runTest {
        val (arrangement, viewModel) = Arrangement().withEstablishedCall().arrange()

        viewModel.getInfoMessage().test {
            viewModel.startRecording()
            runCurrent()

            assertEquals(RecordAudioInfoMessageType.UnableToRecordAudioCall.uiText, awaitItem())
            coVerify(exactly = 0) { arrangement.audioRecorder.start(any()) }
        }
    }

    @Test
    fun `successful start exposes common recording state`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.startRecording()
        runCurrent()

        assertEquals(RecordingButtonState.RECORDING, viewModel.state.buttonState)
        assertEquals(arrangement.originalPath, viewModel.state.originalOutputFile)
        coVerify(exactly = 1) { arrangement.getAssetSizeLimit(false) }
        coVerify(exactly = 1) { arrangement.audioRecorder.start(ASSET_SIZE_LIMIT) }
    }

    @Test
    fun `unsupported recorder remains explicit and reports an error`() = runTest {
        val (_, viewModel) = Arrangement()
            .withStartResult(RecordingCapabilityResult.Unsupported)
            .arrange()

        viewModel.getInfoMessage().test {
            viewModel.startRecording()
            runCurrent()

            assertEquals(
                RecordingAvailability.Unsupported(RecordingCapability.RECORDING),
                viewModel.state.availability,
            )
            assertEquals(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText, awaitItem())
        }
    }

    @Test
    fun `stopping filtered recording delegates effects and exposes ready state`() = runTest {
        val (arrangement, viewModel) = Arrangement().withEffectsEnabled(true).arrange()
        runCurrent()
        viewModel.startRecording()
        runCurrent()

        viewModel.stopRecording()
        runCurrent()

        assertEquals(RecordingButtonState.READY_TO_SEND, viewModel.state.buttonState)
        assertEquals(arrangement.effectsPath, viewModel.getPlayableAudioFile())
        coVerify(exactly = 1) { arrangement.effectsProcessor.process(any()) }
        assertEquals(AudioState.TotalTimeInMs.Known(DURATION_MS), viewModel.state.audioState.totalTimeInMs)
    }

    @Test
    fun `background transition stops active recording`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.startRecording()
        runCurrent()

        arrangement.currentScreen.value = CurrentScreen.InBackground
        runCurrent()

        coVerify(exactly = 1) { arrangement.audioRecorder.stop() }
        assertEquals(RecordingButtonState.READY_TO_SEND, viewModel.state.buttonState)
    }

    @Test
    fun `call starting while recording stops it`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.startRecording()
        runCurrent()

        arrangement.calls.value = listOf(DUMMY_CALL.copy(status = CallStatus.ESTABLISHED))
        runCurrent()

        coVerify(exactly = 1) { arrangement.audioRecorder.stop() }
        assertEquals(RecordingButtonState.READY_TO_SEND, viewModel.state.buttonState)
    }

    @Test
    fun `maximum size event stops and shows size dialog`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.startRecording()
        runCurrent()

        arrangement.recorderEvents.emit(AudioRecorderEvent.MaxFileSizeReached(ASSET_SIZE_LIMIT))
        runCurrent()

        assertEquals(RecordingDialogState.MaxFileSizeReached(5), viewModel.state.maxFileSizeReachedDialogState)
        coVerify(exactly = 1) { arrangement.audioRecorder.stop() }
    }

    @Test
    fun `discard returns common action`() = runTest {
        val (_, viewModel) = Arrangement().arrange()
        viewModel.startRecording()
        runCurrent()

        viewModel.actions.test {
            viewModel.discardRecording()
            runCurrent()

            assertEquals(RecordingAction.Discarded, awaitItem())
            assertEquals(RecordingButtonState.ENABLED, viewModel.state.buttonState)
        }
    }

    @Test
    fun `send returns common path result without Android uri`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.startRecording()
        runCurrent()
        viewModel.stopRecording()
        runCurrent()

        viewModel.actions.test {
            viewModel.sendRecording()
            runCurrent()

            val action = awaitItem() as RecordingAction.Recorded
            assertEquals(arrangement.encodedPath, action.recording.path)
            assertEquals(M4A_AUDIO_MIME_TYPE, action.recording.mimeType)
            coVerify(exactly = 1) {
                arrangement.audioRecorder.encode(arrangement.originalPath, arrangement.encodedPath)
            }
        }
    }

    @Test
    fun `preview actions use narrow preview capability`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.startRecording()
        runCurrent()
        viewModel.stopRecording()
        runCurrent()

        viewModel.onPlayAudio()
        viewModel.onSliderPositionChange(123)

        verify(exactly = 1) { arrangement.recordingPreview.toggle(arrangement.originalPath) }
        verify(exactly = 1) { arrangement.recordingPreview.seekTo(123) }
    }

    @Test
    fun `effects preference is persisted through semantic coordinator action`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.setShouldApplyEffects(true)
        runCurrent()

        coVerify(exactly = 1) { arrangement.globalDataStore.setRecordAudioEffectsCheckboxEnabled(true) }
        assertTrue(viewModel.state.shouldApplyEffects)
    }

    private class Arrangement {
        val audioRecorder = mockk<AudioRecorder>()
        val recordingPreview = mockk<RecordingPreview>()
        val effectsProcessor = mockk<AudioEffectsProcessor>()
        val mediaMetadataReader = mockk<MediaMetadataReader>()
        val observeEstablishedCalls = mockk<ObserveEstablishedCallsUseCase>()
        val currentScreenManager = mockk<CurrentScreenManager>()
        val getAssetSizeLimit = mockk<GetAssetSizeLimitUseCase>()
        val globalDataStore = mockk<GlobalDataStore>()
        val audioNormalizedLoudnessBuilder = mockk<AudioNormalizedLoudnessBuilder>()
        val fileSystem = FakeKaliumFileSystem()
        val recorderEvents = MutableSharedFlow<AudioRecorderEvent>(extraBufferCapacity = 1)
        val previewState = MutableStateFlow(AudioState.DEFAULT)
        val currentScreen = MutableStateFlow<CurrentScreen>(CurrentScreen.Conversation(DUMMY_CALL.conversationId))
        val calls = MutableStateFlow<List<Call>>(emptyList())
        val effectsEnabled = MutableStateFlow(false)
        val originalPath = "/tmp/recording.wav".toPath()
        val encodedPath = "/tmp/recording.m4a".toPath()
        val effectsPath = "/tmp/recording-filter.wav".toPath()
        private var startResult: RecordingCapabilityResult<AudioRecordingFiles> =
            RecordingCapabilityResult.Success(AudioRecordingFiles(originalPath, encodedPath))

        private val viewModel by lazy {
            RecordAudioViewModel(
                observeEstablishedCalls = observeEstablishedCalls,
                getAssetSizeLimit = getAssetSizeLimit,
                currentScreenManager = currentScreenManager,
                audioRecorder = audioRecorder,
                recordingPreview = recordingPreview,
                effectsProcessor = effectsProcessor,
                mediaMetadataReader = mediaMetadataReader,
                globalDataStore = globalDataStore,
                audioNormalizedLoudnessBuilder = audioNormalizedLoudnessBuilder,
                kaliumFileSystem = fileSystem,
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getAssetSizeLimit(false) } returns ASSET_SIZE_LIMIT
            every { audioRecorder.events } returns recorderEvents
            coEvery { audioRecorder.start(any()) } answers { startResult }
            coEvery { audioRecorder.stop() } returns RecordingCapabilityResult.Success(Unit)
            coEvery { audioRecorder.encode(any(), any()) } returns RecordingCapabilityResult.Success(encodedPath)
            every { audioRecorder.release() } returns Unit
            every { recordingPreview.state } returns previewState
            every { recordingPreview.toggle(any()) } returns RecordingCapabilityResult.Success(Unit)
            every { recordingPreview.seekTo(any()) } returns RecordingCapabilityResult.Success(Unit)
            every { recordingPreview.stop() } returns RecordingCapabilityResult.Success(Unit)
            every { recordingPreview.release() } returns Unit
            coEvery { effectsProcessor.process(any()) } answers {
                RecordingCapabilityResult.Success(firstArg<com.wire.media.recording.AudioEffectsRequest>().destination)
            }
            coEvery { mediaMetadataReader.read(any(), any()) } returns PlatformResult.Success(
                AssetContent.AssetMetadata.Audio(DURATION_MS.toLong(), null)
            )
            coEvery { audioNormalizedLoudnessBuilder(any()) } returns byteArrayOf(1, 2)
            coEvery { observeEstablishedCalls() } returns calls
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns currentScreen
            every { globalDataStore.isRecordAudioEffectsCheckboxEnabled() } returns effectsEnabled
            coEvery { globalDataStore.setRecordAudioEffectsCheckboxEnabled(any()) } returns Unit
        }

        fun withEstablishedCall() = apply {
            calls.value = listOf(DUMMY_CALL.copy(status = CallStatus.ESTABLISHED))
        }

        fun withEffectsEnabled(enabled: Boolean) = apply {
            effectsEnabled.value = enabled
        }

        fun withStartResult(result: RecordingCapabilityResult<AudioRecordingFiles>) = apply {
            startResult = result
        }

        fun arrange() = this to viewModel
    }

    private companion object {
        const val ASSET_SIZE_LIMIT = 5L * 1_024L * 1_024L
        const val DURATION_MS = 1_234
        val DUMMY_CALL = Call(
            conversationId = ConversationId("conversationId", "conversationDomain"),
            status = CallStatus.CLOSED,
            callerId = UserId("caller", "domain"),
            participants = emptyList(),
            isMuted = true,
            isCameraOn = false,
            isCbrEnabled = false,
            maxParticipants = 0,
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.OneOnOne,
            callerName = "otherUsername",
            callerTeamName = "team_1",
        )
    }
}
