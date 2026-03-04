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

package com.wire.android.ui.calling

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.OngoingCallState
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.call.CallQuality
import com.wire.kalium.logic.data.call.CallQualityData
import com.wire.kalium.logic.data.call.CallResolutionQuality
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveCallQualityDataUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class OngoingCallViewModelTest {

    @Test
    fun givenAnOngoingCall_WhenTurningOnCamera_ThenSetVideoSendStateToStarted() = runTest {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()

        ongoingCallViewModel.startSendingVideoFeed()

        coVerify(exactly = 1) { arrangement.setVideoSendState(any(), VideoState.STARTED) }
    }

    @Test
    fun givenAnOngoingCall_WhenTurningOffCamera_ThenSetVideoSendStateToStopped() = runTest {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()

        ongoingCallViewModel.stopSendingVideoFeed()

        coVerify { arrangement.setVideoSendState.invoke(any(), VideoState.STOPPED) }
    }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStream_ThenRequestItForOnlyParticipantsWithVideoEnabled() =
        runTest {
            val expectedClients = listOf(
                CallClient(participant1.id.toString(), participant1.clientId),
                CallClient(participant3.id.toString(), participant3.clientId)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .arrange()

            ongoingCallViewModel.requestVideoStreams(participants)

            coVerify(exactly = 1) {
                arrangement.requestVideoStreams(
                    conversationId,
                    expectedClients
                )
            }
        }

    @Test
    fun givenDoubleTabIndicatorIsDisplayed_whenUserTapsOnIt_thenHideIt() = runTest {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withSetShouldShowDoubleTapToastStatus(currentUserId.toString(), false)
            .arrange()

        ongoingCallViewModel.hideDoubleTapToast()

        assertEquals(false, ongoingCallViewModel.shouldShowDoubleTapToast)
        coVerify(exactly = 1) {
            arrangement.globalDataStore.setShouldShowDoubleTapToastStatus(
                currentUserId.toString(),
                false
            )
        }
    }

    @Test
    fun givenSetVideoSendStateUseCase_whenStartSendingVideoFeedIsCalled_thenInvokeUseCaseWithStartedStateOnce() =
        runTest {
            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .arrange()

            ongoingCallViewModel.startSendingVideoFeed()

            coVerify(exactly = 1) {
                arrangement.setVideoSendState(any(), VideoState.STARTED)
            }
        }

    @Test
    fun givenSetVideoSendStateUseCase_whenPauseSendingVideoFeedIsCalled_thenInvokeUseCaseWithPausedStateOnce() =
        runTest {
            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .arrange()

            ongoingCallViewModel.pauseSendingVideoFeed()

            coVerify(exactly = 1) {
                arrangement.setVideoSendState(any(), VideoState.PAUSED)
            }
        }

    @Test
    fun givenSetVideoSendStateUseCase_whenStopSendingVideoFeedIsCalled_thenInvokeUseCaseWithStoppedState() =
        runTest {
            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall().copy(isCameraOn = true))
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .arrange()

            ongoingCallViewModel.stopSendingVideoFeed()

            coVerify(exactly = 1) {
                arrangement.setVideoSendState(any(), VideoState.STOPPED)
            }
        }

    @Test
    fun givenAUserIsSelected_whenRequestedFullScreen_thenSetTheUserAsSelected() =
        runTest {
            val (_, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall().copy(isCameraOn = true))
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .arrange()

            ongoingCallViewModel.onSelectedParticipant(selectedParticipant3)

            assertEquals(selectedParticipant3, ongoingCallViewModel.selectedParticipant)
        }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStreamForFullScreenParticipant_ThenRequestItInHighQuality() =
        runTest {
            val expectedClients = listOf(
                CallClient(participant1.id.toString(), participant1.clientId, false, CallResolutionQuality.LOW),
                CallClient(participant3.id.toString(), participant3.clientId, false, CallResolutionQuality.HIGH)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .arrange()

            ongoingCallViewModel.onSelectedParticipant(selectedParticipant3)
            ongoingCallViewModel.requestVideoStreams(participants)

            coVerify(exactly = 1) {
                arrangement.requestVideoStreams(
                    conversationId,
                    expectedClients
                )
            }
        }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStreamForAllParticipant_ThenRequestItInLowQuality() =
        runTest {
            val expectedClients = listOf(
                CallClient(participant1.id.toString(), participant1.clientId, false, CallResolutionQuality.LOW),
                CallClient(participant3.id.toString(), participant3.clientId, false, CallResolutionQuality.LOW)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .arrange()

            ongoingCallViewModel.onSelectedParticipant(SelectedParticipant())
            ongoingCallViewModel.requestVideoStreams(participants)

            coVerify(exactly = 1) {
                arrangement.requestVideoStreams(
                    conversationId,
                    expectedClients
                )
            }
        }

    @Test
    fun givenActiveOngoingCall_WhenObservingState_ThenStateShouldBeSetToDefault() = runTest {
        val (_, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()
        advanceUntilIdle()

        assertEquals(OngoingCallState.FlowState.Default, ongoingCallViewModel.state.flowState)
    }

    @Test
    fun givenClosedOngoingCall_WhenObservingState_ThenStateShouldBeSetToCallClosed() = runTest {
        val (_, ongoingCallViewModel) = Arrangement()
            .withNoLastActiveCall()
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()
        advanceUntilIdle()

        assertEquals(OngoingCallState.FlowState.CallClosed, ongoingCallViewModel.state.flowState)
    }

    @Test
    fun givenCallQualityChanges_WhenObservingQualityState_ThenStateIsUpdated() = runTest {
        val initialQuality = CallQualityData(CallQuality.NORMAL, 100, 0, 0)
        val callQualityFlow = MutableStateFlow(initialQuality)
        val (_, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withCallQualityDataFlow(callQualityFlow)
            .arrange()
        advanceUntilIdle()
        assertEquals(initialQuality, ongoingCallViewModel.state.callQualityData)

        val changedQuality = CallQualityData(CallQuality.POOR, 300, 10, 15)
        callQualityFlow.value = changedQuality
        advanceUntilIdle()
        assertEquals(changedQuality, ongoingCallViewModel.state.callQualityData)
    }

    private class Arrangement {

        @MockK
        private lateinit var observeLastActiveCall: ObserveLastActiveCallWithSortedParticipantsUseCase

        @MockK
        lateinit var requestVideoStreams: RequestVideoStreamsUseCase

        @MockK
        lateinit var setVideoSendState: SetVideoSendStateUseCase

        @MockK
        lateinit var observeCallQualityData: ObserveCallQualityDataUseCase

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        private val ongoingCallViewModel by lazy {
            OngoingCallViewModel(
                conversationId = conversationId,
                observeLastActiveCall = observeLastActiveCall,
                requestVideoStreams = requestVideoStreams,
                currentUserId = currentUserId,
                setVideoSendState = setVideoSendState,
                globalDataStore = globalDataStore,
                observeCallQualityData = observeCallQualityData
            )
        }

        init {
            MockKAnnotations.init(this)
        }

        fun arrange() = this to ongoingCallViewModel

        fun withNoLastActiveCall() = apply {
            coEvery { observeLastActiveCall(any()) } returns flowOf(null)
        }

        fun withLastActiveCall(call: Call) = apply {
            coEvery { observeLastActiveCall(any()) } returns flowOf(call)
        }

        fun withShouldShowDoubleTapToastReturning(shouldShow: Boolean) = apply {
            coEvery { globalDataStore.getShouldShowDoubleTapToast(any()) } returns shouldShow
        }

        fun withSetVideoSendState() = apply {
            coEvery { setVideoSendState.invoke(any(), any()) } returns Unit
        }

        fun withRequestVideoStreams(conversationId: ConversationId, clients: List<CallClient>) =
            apply {
                coEvery { requestVideoStreams.invoke(conversationId, clients) } returns Unit
            }

        fun withSetShouldShowDoubleTapToastStatus(userId: String, shouldShow: Boolean) = apply {
            coEvery {
                globalDataStore.setShouldShowDoubleTapToastStatus(
                    userId,
                    shouldShow
                )
            } returns Unit
        }

        fun withCallQualityDataFlow(callQualityDataFlow: Flow<CallQualityData>) = apply {
            coEvery { observeCallQualityData(any()) } returns callQualityDataFlow
        }
    }

    companion object {
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val currentUserId = UserId("userId", "some.dummy.domain")
        private val participant1 = UICallParticipant(
            id = QualifiedID("value1", "domain"),
            clientId = "client-id1",
            isSelfUser = false,
            name = "name1",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = true,
            isSharingScreen = false,
            membership = Membership.None,
            hasEstablishedAudio = true,
            accentId = -1
        )
        private val participant2 = UICallParticipant(
            id = QualifiedID("value2", "domain"),
            clientId = "client-id2",
            isSelfUser = false,
            name = "name2",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = false,
            isSharingScreen = false,
            membership = Membership.None,
            hasEstablishedAudio = true,
            accentId = -1
        )
        private val participant3 = UICallParticipant(
            id = QualifiedID("value3", "domain"),
            clientId = "client-id3",
            isSelfUser = false,
            name = "name3",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = true,
            isSharingScreen = true,
            membership = Membership.None,
            hasEstablishedAudio = true,
            accentId = -1
        )
        val participants = listOf(participant1, participant2, participant3)
        val selectedParticipant3 = SelectedParticipant(participant3.id, participant3.clientId, false)
    }

    private fun provideCall(
        id: ConversationId = ConversationId(
            "some-dummy-value",
            "some.dummy.domain"
        )
    ) = Call(
        conversationId = id,
        status = CallStatus.ESTABLISHED,
        callerId = UserId("caller", "domain"),
        participants = listOf(),
        isMuted = false,
        isCameraOn = false,
        maxParticipants = 0,
        conversationName = "ONE_ON_ONE Name",
        conversationType = Conversation.Type.OneOnOne,
        callerName = "otherUsername",
        callerTeamName = "team_1",
        isCbrEnabled = false
    )
}
