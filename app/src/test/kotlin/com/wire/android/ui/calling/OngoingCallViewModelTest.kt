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

import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.TestUser
import com.wire.android.ui.calling.OngoingCallViewModelTest.Arrangement
import com.wire.android.ui.calling.model.ReactionSender
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.call.CallQuality
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.call.InCallReactionMessage
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(NavigationTestExtension::class)
@ExtendWith(CoroutineTestExtension::class)
class OngoingCallViewModelTest {

    @Test
    fun givenAnOngoingCall_WhenTurningOnCamera_ThenSetVideoSendStateToStarted() = runTest {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendSuccess()
            .withNoIncomingReactions()
            .arrange()

        ongoingCallViewModel.startSendingVideoFeed()

        coVerify(exactly = 1) { arrangement.setVideoSendState(any(), VideoState.STARTED) }
    }

    @Test
    fun givenAnOngoingCall_WhenTurningOffCamera_ThenSetVideoSendStateToStopped() = runTest {
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendSuccess()
            .withNoIncomingReactions()
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
                .withCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .withReactionSendSuccess()
                .withNoIncomingReactions()
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
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withSetShouldShowDoubleTapToastStatus(currentUserId.toString(), false)
            .withReactionSendSuccess()
            .withNoIncomingReactions()
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
                .withCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withReactionSendSuccess()
                .withNoIncomingReactions()
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
                .withCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withReactionSendSuccess()
                .withNoIncomingReactions()
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
                .withCall(provideCall().copy(isCameraOn = true))
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withReactionSendSuccess()
                .withNoIncomingReactions()
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
                .withCall(provideCall().copy(isCameraOn = true))
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withReactionSendSuccess()
                .withNoIncomingReactions()
                .arrange()

            ongoingCallViewModel.onSelectedParticipant(selectedParticipant3)

            assertEquals(selectedParticipant3, ongoingCallViewModel.selectedParticipant)
        }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStreamForFullScreenParticipant_ThenRequestItInHighQuality() =
        runTest {
            val expectedClients = listOf(
                CallClient(participant1.id.toString(), participant1.clientId, false, CallQuality.LOW),
                CallClient(participant3.id.toString(), participant3.clientId, false, CallQuality.HIGH)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .withReactionSendSuccess()
                .withNoIncomingReactions()
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
                CallClient(participant1.id.toString(), participant1.clientId, false, CallQuality.LOW),
                CallClient(participant3.id.toString(), participant3.clientId, false, CallQuality.LOW)
            )

            val (arrangement, ongoingCallViewModel) = Arrangement()
                .withCall(provideCall())
                .withShouldShowDoubleTapToastReturning(false)
                .withSetVideoSendState()
                .withRequestVideoStreams(conversationId, expectedClients)
                .withReactionSendSuccess()
                .withNoIncomingReactions()
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
    fun givenAnOngoingCall_WhenInCallReactionIsReceived_ThenNewEmojiIsEmitted() = runTest {

        val reactionsFlow = MutableSharedFlow<InCallReactionMessage>()

        // given
        val (_, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendSuccess()
            .withInCallReactions(reactionsFlow)
            .arrange()

        ongoingCallViewModel.inCallReactions.test {

            // when
            reactionsFlow.emit(InCallReactionMessage(emojis = setOf("👍", "🎉"), senderUserId = TestUser.USER_ID, "Test User"))

            val reaction1 = awaitItem()
            val reaction2 = awaitItem()

            // then
            assertEquals("👍", reaction1.emoji)
            assertEquals("Test User", (reaction1.sender as ReactionSender.Other).name)
            assertEquals("🎉", reaction2.emoji)
            assertEquals("Test User", (reaction2.sender as ReactionSender.Other).name)
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsReceived_ThenNewRecentReactionEmitted() = runTest {

        val reactionsFlow = MutableSharedFlow<InCallReactionMessage>()

        // given
        val (_, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendSuccess()
            .withInCallReactions(reactionsFlow)
            .arrange()

        // when
        reactionsFlow.emit(InCallReactionMessage(emojis = setOf("👍"), senderUserId = TestUser.USER_ID, "Test User"))

        val recentReaction = ongoingCallViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("👍", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenNewInCallReactionIsReceived_ThenRecentReactionUpdated() = runTest {

        val reactionsFlow = MutableSharedFlow<InCallReactionMessage>()

        // given
        val (_, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendSuccess()
            .withInCallReactions(reactionsFlow)
            .arrange()

        // when
        reactionsFlow.emit(InCallReactionMessage(emojis = setOf("👍", "🎉"), senderUserId = TestUser.USER_ID, "Test User"))

        val recentReaction = ongoingCallViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("🎉", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenReactionMessageIsSent() = runTest {
        // given
        val (arrangement, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendSuccess()
            .withNoIncomingReactions()
            .arrange()

        // when
        ongoingCallViewModel.onReactionClick("👍")

        // then
        coVerify(exactly = 1) {
            arrangement.sendInCallReactionUseCase(conversationId, "👍")
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenNewEmojiIsEmitted() = runTest {
        // given
        val (_, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendSuccess()
            .withNoIncomingReactions()
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
    fun givenAnOngoingCall_WhenInCallReactionSentFails_ThenNoEmojiIsEmitted() = runTest {
        // given
        val (_, ongoingCallViewModel) = Arrangement()
            .withCall(provideCall())
            .withShouldShowDoubleTapToastReturning(false)
            .withSetVideoSendState()
            .withReactionSendFailure()
            .withNoIncomingReactions()
            .arrange()

        ongoingCallViewModel.inCallReactions.test {
            // when
            ongoingCallViewModel.onReactionClick("👍")

            // then
            expectNoEvents()
        }
    }

    private class Arrangement {

        @MockK
        private lateinit var establishedCall: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var requestVideoStreams: RequestVideoStreamsUseCase

        @MockK
        lateinit var setVideoSendState: SetVideoSendStateUseCase

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var observeInCallReactionsUseCase: ObserveInCallReactionsUseCase

        @MockK
        lateinit var sendInCallReactionUseCase: SendInCallReactionUseCase

        private val ongoingCallViewModel by lazy {
            OngoingCallViewModel(
                conversationId = conversationId,
                establishedCalls = establishedCall,
                requestVideoStreams = requestVideoStreams,
                currentUserId = currentUserId,
                setVideoSendState = setVideoSendState,
                globalDataStore = globalDataStore,
                observeInCallReactionsUseCase = observeInCallReactionsUseCase,
                sendInCallReactionUseCase = sendInCallReactionUseCase,
            )
        }

        init {
            MockKAnnotations.init(this)
        }

        fun arrange() = this to ongoingCallViewModel

        fun withCall(call: Call) = apply {
            coEvery { establishedCall() } returns flowOf(listOf(call))
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

        fun withReactionSendSuccess() = apply {
            coEvery {
                sendInCallReactionUseCase(conversationId, any())
            } returns Either.Right(Unit)
        }

        fun withReactionSendFailure() = apply {
            coEvery {
                sendInCallReactionUseCase(conversationId, any())
            } returns Either.Left(NetworkFailure.NoNetworkConnection(IllegalStateException()))
        }

        fun withNoIncomingReactions() = apply {
            coEvery {
                observeInCallReactionsUseCase()
            } returns emptyFlow()
        }

        fun withInCallReactions(flow: Flow<InCallReactionMessage>) = apply {
            coEvery {
                observeInCallReactionsUseCase()
            } returns flow
        }
    }

    companion object {
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val currentUserId = UserId("userId", "some.dummy.domain")
        private val participant1 = UICallParticipant(
            id = QualifiedID("value1", "domain"),
            clientId = "client-id1",
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
        conversationType = Conversation.Type.ONE_ON_ONE,
        callerName = "otherUsername",
        callerTeamName = "team_1",
        isCbrEnabled = false
    )
}
