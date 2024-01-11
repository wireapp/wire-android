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

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCaseImpl
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class RecordAudioViewModelTest {

    @Test
    fun `given user is in a call, when start recording audio, then a info message will be shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withEstablishedCall()
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
            val (_, viewModel) = Arrangement()
                .arrange()

            // when
            viewModel.startRecording()

            // then
            assertEquals(
                RecordAudioButtonState.RECORDING,
                viewModel.getButtonState()
            )
        }

    @Test
    fun `given user is recording audio, when stopping the recording, then send audio button is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange()

            viewModel.startRecording()

            // when
            viewModel.stopRecording()

            // then
            assertEquals(
                RecordAudioButtonState.READY_TO_SEND,
                viewModel.getButtonState()
            )
        }

    @Test
    fun `given user is not recording, when closing audio recording view, then verify that close recording view is called`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange()

            // when
            viewModel.showDiscardRecordingDialog {
                // then
                assertEquals(
                    RecordAudioDialogState.Hidden,
                    viewModel.getDiscardDialogState()
                )
            }
        }

    @Test
    fun `given user is recording, when closing audio recording view, then discard audio recording dialog is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange()

            viewModel.startRecording()

            // when
            viewModel.showDiscardRecordingDialog {
                // then
                assertEquals(
                    RecordAudioDialogState.Shown,
                    viewModel.getDiscardDialogState()
                )
            }
        }

    @Test
    fun `given discard audio dialog is shown, when dismissing the dialog, then audio recording dialog is hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange()

            // when
            viewModel.onDismissDiscardDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Hidden,
                viewModel.getDiscardDialogState()
            )
        }

    @Test
    fun `given user doesn't have audio permissions, when starting to record audio, then permissions dialog is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange()

            // when
            viewModel.showPermissionsDeniedDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Shown,
                viewModel.getPermissionsDeniedDialogState()
            )
        }

    @Test
    fun `given permissions dialog is shown, when user dismiss the dialog, then permissions dialog is hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange()

            // when
            viewModel.onDismissPermissionsDeniedDialog()

            // then
            assertEquals(
                RecordAudioDialogState.Hidden,
                viewModel.getPermissionsDeniedDialogState()
            )
        }

    @Test
    fun `given user recorded an audio, when discarding the audio, then file is deleted`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange()

            viewModel.startRecording()
            viewModel.stopRecording()

            // when
            viewModel.discardRecording {
                // then
                assertEquals(
                    RecordAudioButtonState.ENABLED,
                    viewModel.getButtonState()
                )
                assertEquals(
                    RecordAudioDialogState.Hidden,
                    viewModel.getDiscardDialogState()
                )
                assertEquals(
                    null,
                    viewModel.getOutputFile()
                )
            }
        }

    private class Arrangement {

        val recordAudioMessagePlayer = mockk<RecordAudioMessagePlayer>()
        val audioMediaRecorder = mockk<AudioMediaRecorder>()
        val observeEstablishedCalls = mockk<ObserveEstablishedCallsUseCase>()
        val currentScreenManager = mockk<CurrentScreenManager>()

        val viewModel by lazy {
            RecordAudioViewModel(
                recordAudioMessagePlayer = recordAudioMessagePlayer,
                observeEstablishedCalls = observeEstablishedCalls,
                currentScreenManager = currentScreenManager,
                audioMediaRecorder = audioMediaRecorder
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            val fakeKaliumFileSystem = FakeKaliumFileSystem()

            every { audioMediaRecorder.setUp() } returns Unit
            every { audioMediaRecorder.startRecording() } returns Unit
            every { audioMediaRecorder.stop() } returns Unit
            every { audioMediaRecorder.release() } returns Unit
            every { audioMediaRecorder.outputFile } returns fakeKaliumFileSystem
                .tempFilePath("temp_recording.mp3")
                .toFile()
            coEvery { audioMediaRecorder.getMaxFileSizeReached() } returns flowOf(
                RecordAudioDialogState.MaxFileSizeReached(
                    maxSize = GetAssetSizeLimitUseCaseImpl.ASSET_SIZE_DEFAULT_LIMIT_BYTES
                )
            )

            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(
                CurrentScreen.Conversation(
                    id = DUMMY_CALL.conversationId
                )
            )

            coEvery { recordAudioMessagePlayer.audioMessageStateFlow } returns flowOf(
                AudioState.DEFAULT
            )
            coEvery { recordAudioMessagePlayer.stop() } returns Unit
            coEvery { recordAudioMessagePlayer.close() } returns Unit

            coEvery { observeEstablishedCalls() } returns flowOf(listOf())
        }

        fun withEstablishedCall() = apply {
            coEvery { observeEstablishedCalls() } returns flowOf(
                listOf(
                    DUMMY_CALL.copy(status = CallStatus.ESTABLISHED)
                )
            )
        }

        fun arrange() = this to viewModel

        companion object {
            val DUMMY_CALL = Call(
                conversationId = ConversationId(
                    value = "conversationId",
                    domain = "conversationDomain"
                ),
                status = CallStatus.CLOSED,
                callerId = "callerId@domain",
                participants = listOf(),
                isMuted = true,
                isCameraOn = false,
                isCbrEnabled = false,
                maxParticipants = 0,
                conversationName = "ONE_ON_ONE Name",
                conversationType = Conversation.Type.ONE_ON_ONE,
                callerName = "otherUsername",
                callerTeamName = "team_1"
            )
        }
    }
}
