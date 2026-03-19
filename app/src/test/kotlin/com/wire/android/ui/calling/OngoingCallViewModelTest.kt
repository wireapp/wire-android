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

@file:Suppress("UnusedFlow")

package com.wire.android.ui.calling

import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.calling.model.ReactionSender
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.OngoingCallState
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.call.CallQualityData
import com.wire.kalium.logic.data.call.CallResolutionQuality
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.call.CallingParticipantsOrderType
import com.wire.kalium.logic.data.call.InCallReactionMessage
import com.wire.kalium.logic.data.call.Participant
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.call.usecase.ObserveCallQualityDataUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.SetCallQualityIntervalUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.logic.feature.message.MessageOperationResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)@ExtendWith(CoroutineTestExtension::class)
class OngoingCallViewModelTest {
    private val dispatchers = TestDispatcherProvider()

    @Test
    fun givenAnOngoingCall_WhenTurningOnCamera_ThenSetVideoSendStateToStarted() = runTest(dispatchers.main()) {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()

        ongoingCallViewModel.startSendingVideoFeed()

        coVerify(exactly = 1) { arrangement.setVideoSendState(any(), VideoState.STARTED) }
    }

    @Test
    fun givenAnOngoingCall_WhenTurningOffCamera_ThenSetVideoSendStateToStopped() = runTest(dispatchers.main()) {
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
        runTest(dispatchers.main()) {
            val expectedClients = listOf(
                CallClient(uiParticipant1.id.toString(), uiParticipant1.clientId),
                CallClient(uiParticipant3.id.toString(), uiParticipant3.clientId)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .arrange()

            ongoingCallViewModel.requestVideoStreams(uiParticipants)

            coVerify(exactly = 1) {
                arrangement.requestVideoStreams(
                    conversationId,
                    expectedClients
                )
            }
        }

    @Test
    fun givenDoubleTabIndicatorIsDisplayed_whenUserTapsOnIt_thenHideIt() = runTest(dispatchers.main()) {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withSetShouldShowDoubleTapToastStatus(currentUserId.toString(), false)
            .arrange()

        ongoingCallViewModel.hideDoubleTapToast()

        assertEquals(false, ongoingCallViewModel.state.shouldShowDoubleTapToast)
        coVerify(exactly = 1) {
            arrangement.globalDataStore.setShouldShowDoubleTapToastStatus(
                currentUserId.toString(),
                false
            )
        }
    }

    @Test
    fun givenSetVideoSendStateUseCase_whenStartSendingVideoFeedIsCalled_thenInvokeUseCaseWithStartedStateOnce() =
        runTest(dispatchers.main()) {
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
        runTest(dispatchers.main()) {
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
        runTest(dispatchers.main()) {
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
        runTest(dispatchers.main()) {
            val (_, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall().copy(isCameraOn = true))
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .arrange()

            ongoingCallViewModel.onSelectedParticipant(selectedParticipant3)

            assertEquals(selectedParticipant3, ongoingCallViewModel.state.selectedParticipant)
        }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStreamForFullScreenParticipant_ThenRequestItInHighQuality() =
        runTest(dispatchers.main()) {
            val expectedClients = listOf(
                CallClient(uiParticipant1.id.toString(), uiParticipant1.clientId, false, CallResolutionQuality.LOW),
                CallClient(uiParticipant3.id.toString(), uiParticipant3.clientId, false, CallResolutionQuality.HIGH)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .arrange()

            ongoingCallViewModel.onSelectedParticipant(selectedParticipant3)
            ongoingCallViewModel.requestVideoStreams(uiParticipants)

            coVerify(exactly = 1) {
                arrangement.requestVideoStreams(
                    conversationId,
                    expectedClients
                )
            }
        }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStreamForAllParticipant_ThenRequestItInLowQuality() =
        runTest(dispatchers.main()) {
            val expectedClients = listOf(
                CallClient(uiParticipant1.id.toString(), uiParticipant1.clientId, false, CallResolutionQuality.LOW),
                CallClient(uiParticipant3.id.toString(), uiParticipant3.clientId, false, CallResolutionQuality.LOW)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withLastActiveCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .arrange()

            ongoingCallViewModel.onSelectedParticipant(null)
            ongoingCallViewModel.requestVideoStreams(uiParticipants)

            coVerify(exactly = 1) {
                arrangement.requestVideoStreams(
                    conversationId,
                    expectedClients
                )
            }
        }

    @Test
    fun givenActiveOngoingCall_WhenObservingState_ThenStateShouldBeSetToDefault() = runTest(dispatchers.main()) {
        val (_, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()
        advanceUntilIdle()

        assertEquals(OngoingCallState.FlowState.Default, ongoingCallViewModel.state.flowState)
    }

    @Test
    fun givenClosedOngoingCall_WhenObservingState_ThenStateShouldBeSetToCallClosed() = runTest(dispatchers.main()) {
        val (_, ongoingCallViewModel) = Arrangement()
            .withNoLastActiveCall()
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()
        advanceUntilIdle()

        assertEquals(OngoingCallState.FlowState.CallClosed, ongoingCallViewModel.state.flowState)
    }

    @Test
    fun givenCallQualityChanges_WhenObservingQualityState_ThenStateIsUpdated() = runTest(dispatchers.main()) {
        val initialQuality = CallQualityData(quality = CallQualityData.Quality.NORMAL, ping = 0)
        val callQualityFlow = MutableStateFlow(initialQuality)
        val (_, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withCallQualityDataFlow(callQualityFlow)
            .arrange()
        advanceUntilIdle()
        assertEquals(initialQuality, ongoingCallViewModel.state.callQualityData)

        val changedQuality = CallQualityData(CallQualityData.Quality.POOR, ping = 300)
        callQualityFlow.value = changedQuality
        advanceUntilIdle()
        assertEquals(changedQuality, ongoingCallViewModel.state.callQualityData)
    }

    @Test
    fun givenCall_WhenChangingCallQualityInterval_ThenSetIntervalProperly() = runTest(dispatchers.main()) {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()
        advanceUntilIdle()

        ongoingCallViewModel.setQualityInterval(OngoingCallViewModel.QualityInterval.SHORT)

        coVerify(exactly = 1) {
            arrangement.setCallQualityInterval(OngoingCallViewModel.QualityInterval.SHORT.intervalInSeconds)
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsReceived_ThenNewEmojiIsEmitted() = runTest(dispatchers.main()) {
        val (arrangement, ongoingCallViewModel) = Arrangement().arrange()

        ongoingCallViewModel.inCallReactions.test {

            // when
            arrangement.reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍", "🎉")))

            val reaction1 = awaitItem()
            val reaction2 = awaitItem()

            // then
            assertEquals("👍", reaction1.emoji)
            assertIs<ReactionSender.Unknown>(reaction1.sender)
            assertEquals("🎉", reaction2.emoji)
            assertIs<ReactionSender.Unknown>(reaction2.sender)
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsReceived_ThenNewRecentReactionEmitted() = runTest(dispatchers.main()) {
        val (arrangement, ongoingCallViewModel) = Arrangement().arrange()

        // when
        arrangement.reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍")))

        val recentReaction = ongoingCallViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("👍", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenNewInCallReactionIsReceived_ThenRecentReactionUpdated() = runTest(dispatchers.main()) {
        val (arrangement, ongoingCallViewModel) = Arrangement().arrange()

        // when
        arrangement.reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍", "🎉")))

        val recentReaction = ongoingCallViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("🎉", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenReactionMessageIsSent() = runTest(dispatchers.main()) {

        // given
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withSendInCallReactionUseCaseReturning(MessageOperationResult.Success)
            .arrange()

        // when
        ongoingCallViewModel.onReactionClick("👍")

        // then
        coVerify(exactly = 1) {
            arrangement.sendInCallReactionUseCase(conversationId, "👍")
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenNewEmojiIsEmitted() = runTest(dispatchers.main()) {

        // given
        val (_, ongoingCallViewModel) = Arrangement()
            .withSendInCallReactionUseCaseReturning(MessageOperationResult.Success)
            .arrange()

        ongoingCallViewModel.inCallReactions.test {
            // when
            ongoingCallViewModel.onReactionClick("👍")

            val reaction = awaitItem()

            // then
            assertEquals("👍", reaction.emoji)
            assertIs<ReactionSender.You>(reaction.sender)
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenReactionMessageIsSentAndAddedToRecentReactions() =
        runTest(dispatchers.main()) {

            // given
            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withSendInCallReactionUseCaseReturning(MessageOperationResult.Success)
                .arrange()

            // when
            ongoingCallViewModel.onReactionClick("👌")

            // then
            coVerify(exactly = 1) {
                arrangement.sendInCallReactionUseCase(conversationId, "👌")
            }
            assertTrue(ongoingCallViewModel.recentReactions.containsValue("👌"))
        }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionSentFails_ThenNoEmojiIsEmitted() = runTest(dispatchers.main()) {
        // given
        val (_, ongoingCallViewModel) = Arrangement()
            .withSendInCallReactionUseCaseReturning(
                MessageOperationResult.Failure(
                    NetworkFailure.NoNetworkConnection(
                        IllegalStateException()
                    )
                )
            )
            .arrange()

        ongoingCallViewModel.inCallReactions.test {
            // when
            ongoingCallViewModel.onReactionClick("👍")

            // then
            expectNoEvents()
        }
    }

    @Test
    fun givenActiveCall_whenParticipantsChange_thenParticipantsStateIsUpdated() = runTest(dispatchers.main()) {
        val callFlow = MutableSharedFlow<Call?>()
        val (_, ongoingCallViewModel) = Arrangement()
            .withLastActiveCallFlow(callFlow)
            .arrange()

        callFlow.emit(provideCall().copy(status = CallStatus.ANSWERED, participants = emptyList()))
        advanceUntilIdle()
        ongoingCallViewModel.state.participants shouldBeEqualTo persistentListOf()

        callFlow.emit(provideCall().copy(status = CallStatus.ESTABLISHED, participants = participants))
        advanceUntilIdle()
        ongoingCallViewModel.state.participants shouldBeEqualTo uiParticipants.toPersistentList()
    }

    @Test
    fun givenCall_WhenDisablingOthersVideos_ThenStateIsUpdated() = runTest(dispatchers.main()) {
        val (_, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()
        advanceUntilIdle()

        ongoingCallViewModel.setOthersVideosDisabled(true)
        advanceUntilIdle()
        assertEquals(true, ongoingCallViewModel.state.othersVideosDisabled)

        ongoingCallViewModel.setOthersVideosDisabled(false)
        advanceUntilIdle()
        assertEquals(false, ongoingCallViewModel.state.othersVideosDisabled)
    }

    @Test
    fun givenCall_WhenDisablingOthersVideos_ThenParticipantsOrderIsUpdated() = runTest(dispatchers.main()) {
        val participantsVideosFirst = listOf(participant3, participant1, participant2)
        val participantsAlphabetically = listOf(participant1, participant2, participant3)
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withLastActiveCall(provideCall().copy(participants = participantsVideosFirst), CallingParticipantsOrderType.VIDEOS_FIRST)
            .withLastActiveCall(provideCall().copy(participants = participantsAlphabetically), CallingParticipantsOrderType.ALPHABETICALLY)
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .arrange()
        advanceUntilIdle()

        ongoingCallViewModel.setOthersVideosDisabled(false)
        advanceUntilIdle()
        val expectedVideosFirst = listOf(uiParticipant3, uiParticipant1, uiParticipant2)
        assertEquals(expectedVideosFirst, ongoingCallViewModel.state.participants)
        coVerify(exactly = 1) {
            arrangement.observeLastActiveCall(any(), eq(CallingParticipantsOrderType.VIDEOS_FIRST))
        }

        ongoingCallViewModel.setOthersVideosDisabled(true)
        advanceUntilIdle()
        val expectedAlphabetically = listOf(uiParticipant1, uiParticipant2, uiParticipant3)
        assertEquals(expectedAlphabetically, ongoingCallViewModel.state.participants)
        coVerify(exactly = 1) {
            arrangement.observeLastActiveCall(any(), eq(CallingParticipantsOrderType.ALPHABETICALLY))
        }
    }

    private inner class Arrangement {

        @MockK
        lateinit var observeLastActiveCall: ObserveLastActiveCallWithSortedParticipantsUseCase

        @MockK
        lateinit var requestVideoStreams: RequestVideoStreamsUseCase

        @MockK
        lateinit var setVideoSendState: SetVideoSendStateUseCase

        @MockK
        lateinit var observeCallQualityData: ObserveCallQualityDataUseCase

        @MockK
        lateinit var setCallQualityInterval: SetCallQualityIntervalUseCase

        @MockK
        lateinit var observeInCallReactionsUseCase: ObserveInCallReactionsUseCase

        @MockK
        lateinit var sendInCallReactionUseCase: SendInCallReactionUseCase

        @MockK
        lateinit var getCurrentClientId: ObserveCurrentClientIdUseCase

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        val reactionsFlow = MutableSharedFlow<InCallReactionMessage>()

        private val ongoingCallViewModel by lazy {
            OngoingCallViewModel(
                conversationId = conversationId,
                observeLastActiveCall = observeLastActiveCall,
                requestVideoStreams = requestVideoStreams,
                currentUserId = currentUserId,
                setVideoSendState = setVideoSendState,
                globalDataStore = globalDataStore,
                observeCallQualityData = observeCallQualityData,
                setCallQualityInterval = setCallQualityInterval,
                observeInCallReactionsUseCase = observeInCallReactionsUseCase,
                sendInCallReactionUseCase = sendInCallReactionUseCase,
                getCurrentClientId = getCurrentClientId,
                uiCallParticipantMapper = UICallParticipantMapper(UserTypeMapper()),
                dispatchers = dispatchers
            )
        }

        init {
            MockKAnnotations.init(this)
            coEvery { observeCallQualityData(any()) } returns emptyFlow()
            coEvery { setCallQualityInterval(any()) } returns Unit
            coEvery { observeInCallReactionsUseCase(any()) } returns reactionsFlow
            coEvery { getCurrentClientId() } returns flowOf(currentClientId)
            coEvery { observeLastActiveCall.invoke(any()) } returns emptyFlow()
        }

        fun arrange() = this to ongoingCallViewModel

        fun withNoLastActiveCall() = apply {
            coEvery { observeLastActiveCall(any(), any()) } returns flowOf(null)
        }

        fun withLastActiveCall(call: Call, orderType: CallingParticipantsOrderType? = null) = apply {
            coEvery { observeLastActiveCall(any(), (orderType ?: any())) } returns flowOf(call)
        }

        fun withLastActiveCallFlow(callFlow: Flow<Call?>) = apply {
            coEvery { observeLastActiveCall(any(), any()) } returns callFlow
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

        fun withSendInCallReactionUseCaseReturning(result: MessageOperationResult) = apply {
            coEvery { sendInCallReactionUseCase(conversationId, any()) } returns result
        }
    }

    companion object {
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val currentUserId = UserId("userId", "some.dummy.domain")
        private val currentClientId = ClientId("current_client_id")
        private val uiParticipant1 = provideUICallParticipant(index = 1, isSelfUser = false, isCameraOn = true, isSharingScreen = false)
        private val uiParticipant2 = provideUICallParticipant(index = 2, isSelfUser = false, isCameraOn = false, isSharingScreen = false)
        private val uiParticipant3 = provideUICallParticipant(index = 3, isSelfUser = false, isCameraOn = true, isSharingScreen = true)
        private val participant1 = provideParticipant(index = 1, isCameraOn = true, isSharingScreen = false)
        private val participant2 = provideParticipant(index = 2, isCameraOn = false, isSharingScreen = false)
        private val participant3 = provideParticipant(index = 3, isCameraOn = true, isSharingScreen = true)
        val uiParticipants = listOf(uiParticipant1, uiParticipant2, uiParticipant3)
        val participants = listOf(participant1, participant2, participant3)
        val selectedParticipant3 = SelectedParticipant(uiParticipant3.id, uiParticipant3.clientId, false)

        fun provideUICallParticipant(index: Int, isSelfUser: Boolean, isCameraOn: Boolean, isSharingScreen: Boolean) = UICallParticipant(
            id = QualifiedID("value$index", "domain"),
            clientId = "client-id$index",
            name = "name$index",
            isMuted = false,
            isCameraOn = isCameraOn,
            hasEstablishedAudio = true,
            isSpeaking = false,
            isSharingScreen = isSharingScreen,
            avatar = null,
            membership = Membership.None,
            isSelfUser = isSelfUser,
            accentId = -1
        )

        fun provideParticipant(index: Int, isCameraOn: Boolean, isSharingScreen: Boolean) = Participant(
            id = QualifiedID("value$index", "domain"),
            clientId = "client-id$index",
            name = "name$index",
            isMuted = false,
            isCameraOn = isCameraOn,
            hasEstablishedAudio = true,
            isSpeaking = false,
            isSharingScreen = isSharingScreen,
            avatarAssetId = null,
            userType = UserTypeInfo.Regular(UserType.NONE),
            accentId = -1
        )
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
