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
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.audiomessage.AudioFocusHelper
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioViewModelTest.Arrangement.Companion.ASSET_SIZE_LIMIT
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.fromNioPathToContentUri
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase.AssetSizeLimits.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

@ExtendWith(CoroutineTestExtension::class)
class RecordAudioViewModelTest {

    @Test
    fun `given user is in a call, when start recording audio, then a info message will be shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withEstablishedCall()
                .withFilterEnabled(false)
                .arrange()

            viewModel.getInfoMessage().test {
                // when
                viewModel.startRecording()

                // then
                val result = awaitItem()
                assertEquals(
                    RecordAudioInfoMessageType.UnableToRecordAudioCall.uiText,
                    result
                )
            }
        }

    @Test
    fun `given user is not in a call, when start recording audio, then recording screen is shown`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.startRecording()

            // then
            assertEquals(
                RecordAudioButtonState.RECORDING,
                viewModel.state.buttonState
            )
            coVerify(exactly = 1) { arrangement.getAssetSizeLimit(false) }
            verify(exactly = 1) { arrangement.audioMediaRecorder.setUp(ASSET_SIZE_LIMIT) }
            verify(exactly = 1) { arrangement.audioMediaRecorder.setUp(ASSET_SIZE_LIMIT) }
            verify(exactly = 1) { arrangement.audioMediaRecorder.startRecording() }
        }

    @Test
    fun `given user is recording audio without filter, when stopping the recording, then send audio button is shown`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()

            // when
            viewModel.stopRecording()

            // then
            coVerify(exactly = 0) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any()
                )
            }
            assertEquals(
                RecordAudioButtonState.READY_TO_SEND,
                viewModel.state.buttonState
            )
        }

    @Test
    fun `given user is recording audio with filter, when stopping the recording, then send audio button is shown`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(true)
                .arrange()

            viewModel.startRecording()

            // when
            viewModel.stopRecording()

            // then
            coVerify(exactly = 1) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any(),
                )
            }
            assertEquals(
                RecordAudioButtonState.READY_TO_SEND,
                viewModel.state.buttonState
            )
        }

    @Test
    fun `given user is recording audio without filter, when applying filter after recording, then effects file is generated`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()
            viewModel.stopRecording()
            assertEquals(null, viewModel.state.effectsOutputFile)
            coVerify(exactly = 0) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any()
                )
            }

            // when
            viewModel.setShouldApplyEffects(true)

            // then
            coVerify(exactly = 1) {
                arrangement.generateAudioFileWithEffects(
                    context = any(),
                    originalFilePath = any(),
                    effectsFilePath = any()
                )
            }
            assert(viewModel.state.effectsOutputFile != null)
            assertEquals(
                RecordAudioButtonState.READY_TO_SEND,
                viewModel.state.buttonState
            )
        }

    @Test
    fun `given user is not recording, when closing audio recording view, then verify that close recording view is called`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.actions.test {
                // when
                viewModel.showDiscardRecordingDialog()
                // then
                assertEquals(
                    RecordAudioDialogState.Hidden,
                    viewModel.state.discardDialogState
                )
                assertEquals(
                    RecordAudioViewActions.Discarded,
                    awaitItem()
                )
            }
        }

    @Test
    fun `given user is recording, when closing audio recording view, then discard audio recording dialog is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()

            viewModel.actions.test {
                // when
                viewModel.showDiscardRecordingDialog()
                // then
                assertEquals(
                    RecordAudioDialogState.Shown,
                    viewModel.state.discardDialogState
                )
                expectNoEvents()
            }
        }

    @Test
    fun `given discard audio dialog is shown, when dismissing the dialog, then audio recording dialog is hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.onDismissDiscardDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Hidden,
                viewModel.state.discardDialogState
            )
        }

    @Test
    fun `given user doesn't have audio permissions, when starting to record audio, then permissions dialog is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.showPermissionsDeniedDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Shown,
                viewModel.state.permissionsDeniedDialogState
            )
        }

    @Test
    fun `given permissions dialog is shown, when user dismiss the dialog, then permissions dialog is hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            // when
            viewModel.onDismissPermissionsDeniedDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Hidden,
                viewModel.state.permissionsDeniedDialogState
            )
        }

    @Test
    fun `given user recorded an audio, when discarding the audio, then file is deleted`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .arrange()

            viewModel.startRecording()
            viewModel.stopRecording()

            viewModel.actions.test {
                // when
                viewModel.discardRecording()
                // then
                assertEquals(
                    RecordAudioButtonState.ENABLED,
                    viewModel.state.buttonState
                )
                assertEquals(
                    RecordAudioDialogState.Hidden,
                    viewModel.state.discardDialogState
                )
                assertEquals(
                    null,
                    viewModel.state.originalOutputFile
                )
                assertEquals(
                    RecordAudioViewActions.Discarded,
                    awaitItem()
                )

                verify(exactly = 1) { arrangement.audioFocusHelper.requestExclusive() }
                verify(exactly = 2) { arrangement.audioFocusHelper.abandonExclusive() } // 1 before start recording, 1 after
            }
        }

    @Test
    fun `given user is recording, when discarding the audio, then recorder is stopped before it is released`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .withMaxFileSizeNeverReached()
                .arrange()

            viewModel.startRecording()

            viewModel.actions.test {
                // when
                viewModel.discardRecording()

                // then
                assertEquals(RecordAudioViewActions.Discarded, awaitItem())
                assertEquals(RecordAudioButtonState.ENABLED, viewModel.state.buttonState)
                coVerifyOrder {
                    arrangement.audioMediaRecorder.stop()
                    arrangement.audioMediaRecorder.release()
                }
            }
        }

    @Test
    fun `given encoding is running, when discarding and the encoding then finishes, then the discard stays`() =
        runTest {
            // given
            val encodingGate = CompletableDeferred<Unit>()
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(true)
                .withEncodingWaitingFor(encodingGate)
                .arrange()
            viewModel.startRecording()
            viewModel.stopRecording()
            assertEquals(RecordAudioButtonState.ENCODING, viewModel.state.buttonState)

            // when
            viewModel.discardRecording()
            encodingGate.complete(Unit)

            // then
            assertEquals(RecordAudioButtonState.ENABLED, viewModel.state.buttonState)
            assertEquals(null, viewModel.state.originalOutputFile)
            assertEquals(null, viewModel.state.effectsOutputFile)
        }

    @Test
    fun `given a stop is in flight, when discarding, then the recorder is still released after the stop finishes`() =
        runTest {
            // given
            val stopGate = CompletableDeferred<Unit>()
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .withMaxFileSizeNeverReached()
                .withFirstRecorderStopWaitingFor(stopGate)
                .arrange()
            viewModel.startRecording()
            viewModel.stopRecording()

            // when
            viewModel.discardRecording()
            stopGate.complete(Unit)

            // then
            assertEquals(RecordAudioButtonState.ENABLED, viewModel.state.buttonState)
            coVerify(exactly = 2) { arrangement.audioMediaRecorder.stop() }
            verify(exactly = 1) { arrangement.audioMediaRecorder.release() }
            coVerifyOrder {
                arrangement.audioMediaRecorder.stop()
                arrangement.audioMediaRecorder.release()
            }
        }

    @Test
    fun `given user is recording, when the view model is cleared, then the recorder is stopped and released`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .withMaxFileSizeNeverReached()
                .arrange()
            viewModel.startRecording()

            // when
            clear(viewModel)

            // then
            coVerifyOrder {
                arrangement.audioMediaRecorder.stop()
                arrangement.audioMediaRecorder.release()
            }
            verify(exactly = 1) { arrangement.audioFocusHelper.abandonExclusive() }
        }

    @Test
    fun `given a stop is in flight, when the view model is cleared, then the recorder is still released`() =
        runTest {
            // given
            val stopGate = CompletableDeferred<Unit>()
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .withMaxFileSizeNeverReached()
                .withFirstRecorderStopWaitingFor(stopGate)
                .arrange()
            viewModel.startRecording()
            viewModel.stopRecording()

            // when
            clear(viewModel)
            stopGate.complete(Unit)

            // then
            verify(exactly = 1) { arrangement.audioMediaRecorder.release() }
            coVerifyOrder {
                arrangement.audioMediaRecorder.stop()
                arrangement.audioMediaRecorder.release()
            }
        }

    @Test
    fun `given encoding is running, when discarding and recording again, then the new recording is not overwritten`() =
        runTest {
            // given
            val encodingGate = CompletableDeferred<Unit>()
            val (_, viewModel) = Arrangement()
                .withFilterEnabled(true)
                .withEncodingWaitingFor(encodingGate)
                .arrange()
            viewModel.startRecording()
            viewModel.stopRecording()
            viewModel.discardRecording()
            viewModel.startRecording()
            assertEquals(RecordAudioButtonState.RECORDING, viewModel.state.buttonState)

            // when
            encodingGate.complete(Unit)

            // then
            assertEquals(RecordAudioButtonState.RECORDING, viewModel.state.buttonState)
        }

    @Test
    fun `given user recorded an audio, when sending the recording, then the recorder is back to its initial state`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(false)
                .withMaxFileSizeNeverReached()
                .arrange()
            val m4aPath = arrangement.fakeKaliumFileSystem.tempFilePath("voice.m4a")
            every { arrangement.audioMediaRecorder.m4aOutputPath } returns m4aPath
            coEvery { arrangement.audioMediaRecorder.convertWavToM4a(any()) } returns true

            mockkStatic("com.wire.android.util.FileUtilKt")
            try {
                every { any<Context>().fromNioPathToContentUri(any()) } returns mockk<Uri>()
                viewModel.startRecording()
                viewModel.stopRecording()

                viewModel.actions.test {
                    // when
                    viewModel.sendRecording()

                    // then
                    assertIs<RecordAudioViewActions.Recorded>(awaitItem())
                    assertEquals(RecordAudioState().buttonState, viewModel.state.buttonState)
                    assertEquals(null, viewModel.state.originalOutputFile)
                    assertEquals(AudioState.DEFAULT, viewModel.state.audioState)
                    assertEquals(null, viewModel.state.wavesMask)
                }
            } finally {
                unmockkStatic("com.wire.android.util.FileUtilKt")
            }
        }

    @Test
    fun `given the conversion fails and effects are off, when sending the recording, then the original wav is sent and kept`(
        @TempDir dir: java.nio.file.Path
    ) = runTest { assertWavFallbackSendsKeptFile(dir, applyEffects = false) }

    @Test
    fun `given the conversion fails and effects are on, when sending the recording, then the effects wav is sent and kept`(
        @TempDir dir: java.nio.file.Path
    ) = runTest { assertWavFallbackSendsKeptFile(dir, applyEffects = true) }

    private suspend fun assertWavFallbackSendsKeptFile(dir: java.nio.file.Path, applyEffects: Boolean) {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withFilterEnabled(applyEffects)
            .withMaxFileSizeNeverReached()
            .arrange()
        coEvery { arrangement.audioMediaRecorder.convertWavToM4a(any()) } returns false
        every { arrangement.audioMediaRecorder.originalOutputPath } returns dir.resolve("temp_recording.wav").toOkioPath()
        every { arrangement.fakeKaliumFileSystem.tempFilePath(any()) } answers { dir.resolve(firstArg<String>()).toOkioPath() }
        val sentUri = mockk<Uri>()
        val sentPaths = mutableListOf<java.nio.file.Path>()
        mockkStatic("com.wire.android.util.FileUtilKt")

        try {
            every { any<Context>().fromNioPathToContentUri(any()) } answers {
                sentPaths.add(secondArg())
                sentUri
            }
            viewModel.startRecording()
            viewModel.stopRecording()
            val original = viewModel.state.originalOutputFile!!.also { it.createNewFile() }
            val effects = viewModel.state.effectsOutputFile?.also { it.createNewFile() }
            val (kept, dropped) = if (applyEffects) effects!! to original else original to effects

            viewModel.actions.test {
                // when
                viewModel.sendRecording()

                // then
                val action = assertIs<RecordAudioViewActions.Recorded>(awaitItem())
                assertEquals("audio/wav", action.uriAsset.mimeType)
                assertEquals(sentUri, action.uriAsset.uri)
                assertEquals(listOf(kept.toPath()), sentPaths)
                assertEquals(true, kept.exists())
                assertEquals(false, dropped?.exists() ?: false)
            }
        } finally {
            unmockkStatic("com.wire.android.util.FileUtilKt")
        }
    }

    @Test
    fun `given start recording succeeded, when recording audio, then recording screen is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withStartRecordingSuccessful()
                .withFilterEnabled(false)
                .arrange()

            viewModel.getInfoMessage().test {
                // when
                viewModel.startRecording()
                // then
                assertEquals(RecordAudioButtonState.RECORDING, viewModel.state.buttonState)
                expectNoEvents()
            }
        }

    @Test
    fun `given start recording failed, when recording audio, then info message is shown`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withStartRecordingFailed()
                .withFilterEnabled(false)
                .arrange()

            viewModel.getInfoMessage().test {
                // when
                viewModel.startRecording()
                // then
                assertEquals(RecordAudioButtonState.ENABLED, viewModel.state.buttonState)
                assertEquals(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText, awaitItem())

                verify(exactly = 1) { arrangement.audioFocusHelper.requestExclusive() }
            }
        }

    @Test
    fun `given the recording holds no audio, when stopping the recording, then the recorder resets and an error is shown`(
        @TempDir dir: java.nio.file.Path
    ) =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withFilterEnabled(true)
                .withMaxFileSizeNeverReached()
                .withEmptyRecording()
                .arrange()
            every { arrangement.fakeKaliumFileSystem.tempFilePath(any()) } answers { dir.resolve(firstArg<String>()).toOkioPath() }

            viewModel.getInfoMessage().test {
                viewModel.startRecording()
                val recording = viewModel.state.originalOutputFile!!
                val effects = viewModel.state.effectsOutputFile!!.also { it.createNewFile() }

                // when
                viewModel.stopRecording()

                // then
                assertEquals(RecordAudioInfoMessageType.UnableToRecordAudioError.uiText, awaitItem())
                assertEquals(RecordAudioButtonState.ENABLED, viewModel.state.buttonState)
                assertEquals(null, viewModel.state.originalOutputFile)
                assertEquals(null, viewModel.state.effectsOutputFile)
                assertEquals(false, recording.exists())
                assertEquals(false, effects.exists())
            }
        }

    private fun clear(viewModel: RecordAudioViewModel) {
        val store = ViewModelStore()
        ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
            }
        )[RecordAudioViewModel::class.java]
        store.clear()
    }

    private class Arrangement {

        val recordAudioMessagePlayer = mockk<RecordAudioMessagePlayer>()
        val audioMediaRecorder = mockk<AudioMediaRecorder>()
        val observeEstablishedCalls = mockk<ObserveEstablishedCallsUseCase>()
        val currentScreenManager = mockk<CurrentScreenManager>()
        val getAssetSizeLimit = mockk<GetAssetSizeLimitUseCase>()
        val globalDataStore = mockk<GlobalDataStore>()
        val generateAudioFileWithEffects = mockk<GenerateAudioFileWithEffectsUseCase>()
        val context = mockk<Context>()
        val dispatchers = TestDispatcherProvider()
        val fakeKaliumFileSystem = spyk(FakeKaliumFileSystem())
        val audioNormalizedLoudnessBuilder = mockk<AudioNormalizedLoudnessBuilder>()
        val audioFocusHelper = mockk<AudioFocusHelper>()
        private val recordingDir = Files.createTempDirectory("record_audio_test").toFile().apply { deleteOnExit() }
        private var recordedAudioBytes = RECORDED_AUDIO_BYTES

        val viewModel by lazy {
            RecordAudioViewModel(
                context = context,
                recordAudioMessagePlayer = recordAudioMessagePlayer,
                observeEstablishedCalls = observeEstablishedCalls,
                currentScreenManager = currentScreenManager,
                audioMediaRecorder = audioMediaRecorder,
                getAssetSizeLimit = getAssetSizeLimit,
                generateAudioFileWithEffects = generateAudioFileWithEffects,
                globalDataStore = globalDataStore,
                dispatchers = dispatchers,
                audioNormalizedLoudnessBuilder = audioNormalizedLoudnessBuilder,
                kaliumFileSystem = fakeKaliumFileSystem,
                audioFocusHelper = audioFocusHelper
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { getAssetSizeLimit.invoke(false) } returns ASSET_SIZE_LIMIT
            every { audioMediaRecorder.setUp(ASSET_SIZE_LIMIT) } returns Unit
            every { audioMediaRecorder.startRecording() } answers { writeRecording() }
            coEvery { audioMediaRecorder.stop() } returns Unit
            every { audioMediaRecorder.release() } returns Unit
            coEvery { globalDataStore.setRecordAudioEffectsCheckboxEnabled(any()) } returns Unit
            every { audioMediaRecorder.originalOutputPath } returns File(recordingDir, "temp_recording.wav").toOkioPath()
            coEvery { audioMediaRecorder.getMaxFileSizeReached() } returns flowOf(
                RecordAudioDialogState.MaxFileSizeReached(
                    maxSize = ASSET_SIZE_DEFAULT_LIMIT_BYTES
                )
            )
            coEvery { generateAudioFileWithEffects(any(), any(), any()) } returns Unit

            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(
                CurrentScreen.Conversation(id = DUMMY_CALL.conversationId)
            )

            coEvery { recordAudioMessagePlayer.audioMessageStateFlow } returns flowOf(
                AudioState.DEFAULT
            )
            coEvery { recordAudioMessagePlayer.stop() } returns Unit
            coEvery { recordAudioMessagePlayer.close() } returns Unit

            coEvery { observeEstablishedCalls() } returns flowOf(listOf())

            coEvery { audioNormalizedLoudnessBuilder(any<String>()) } returns byteArrayOf()

            every { audioFocusHelper.requestExclusive() } returns true
            every { audioFocusHelper.abandonExclusive() } returns Unit
        }

        fun withEstablishedCall() = apply {
            coEvery { observeEstablishedCalls() } returns flowOf(
                listOf(
                    DUMMY_CALL.copy(status = CallStatus.ESTABLISHED)
                )
            )
        }

        fun withStartRecordingSuccessful() = apply {
            every { audioMediaRecorder.startRecording() } answers { writeRecording() }
        }
        fun withStartRecordingFailed() = apply { every { audioMediaRecorder.startRecording() } returns false }

        fun withEncodingWaitingFor(gate: CompletableDeferred<Unit>) = apply {
            coEvery { generateAudioFileWithEffects(any(), any(), any()) } coAnswers { gate.await() }
        }

        fun withFirstRecorderStopWaitingFor(gate: CompletableDeferred<Unit>) = apply {
            var isFirstStop = true
            coEvery { audioMediaRecorder.stop() } coAnswers {
                if (isFirstStop) {
                    isFirstStop = false
                    gate.await()
                }
            }
        }

        fun withEmptyRecording() = apply { recordedAudioBytes = 0 }

        // the real recorder writes the WAV header, then the audio, to originalOutputPath
        private fun writeRecording(): Boolean {
            val recording = audioMediaRecorder.originalOutputPath!!.toFile().apply { deleteOnExit() }
            recording.writeBytes(ByteArray(AudioMediaRecorder.HEADER_SIZE + recordedAudioBytes))
            return true
        }

        fun withMaxFileSizeNeverReached() = apply {
            every { audioMediaRecorder.getMaxFileSizeReached() } returns emptyFlow()
        }

        fun withFilterEnabled(isEnabled: Boolean) = apply {
            every { globalDataStore.isRecordAudioEffectsCheckboxEnabled() } returns flowOf(isEnabled)
        }

        fun arrange() = this to viewModel

        companion object {
            const val ASSET_SIZE_LIMIT = 5L
            const val RECORDED_AUDIO_BYTES = 1024
            val DUMMY_CALL = Call(
                conversationId = ConversationId(
                    value = "conversationId",
                    domain = "conversationDomain"
                ),
                status = CallStatus.CLOSED,
                callerId = UserId("caller", "domain"),
                participants = listOf(),
                isMuted = true,
                isCameraOn = false,
                isCbrEnabled = false,
                maxParticipants = 0,
                conversationName = "ONE_ON_ONE Name",
                conversationType = Conversation.Type.OneOnOne,
                callerName = "otherUsername",
                callerTeamName = "team_1"
            )
        }
    }
}
