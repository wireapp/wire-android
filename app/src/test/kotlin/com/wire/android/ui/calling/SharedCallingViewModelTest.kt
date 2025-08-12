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

import android.view.Surface
import android.view.View
import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.calling.common.SharedCallingViewActions
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.model.ReactionSender
import com.wire.android.ui.calling.usecase.HangUpCallUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.call.InCallReactionMessage
import com.wire.kalium.logic.data.call.Participant
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetUIRotationUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.logic.util.PlatformRotation
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class SharedCallingViewModelTest {
    private val dispatchers = TestDispatcherProvider()

    @Test
    fun `given isMuted value is null, when toggling microphone, then do not update microphone state`() =
        runTest(dispatchers.main()) {
            val (_, sharedCallingViewModel) = Arrangement().arrange()
            sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = null)

            sharedCallingViewModel.toggleMute()

            sharedCallingViewModel.callState.isMuted shouldBeEqualTo null
        }

    @Test
    fun `given an un-muted call, when toggling microphone, then mute the call`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)

        sharedCallingViewModel.toggleMute()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.muteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given a muted call, when toggling microphone, then un-mute the call`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)

        sharedCallingViewModel.toggleMute()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.unMuteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
    }

    @Test
    fun `given user on a preview screen, when muting microphone, then mute the call with false param`() =
        runTest(dispatchers.main()) {
            val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
            sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)

            sharedCallingViewModel.toggleMute(true)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.muteCall(any(), false) }
            sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
        }

    @Test
    fun `given user on a preview screen, when un-muting microphone, then un-mute the call with false param`() =
        runTest(dispatchers.main()) {
            val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
            sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)

            sharedCallingViewModel.toggleMute(true)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.unMuteCall(any(), false) }
            sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
        }

    @Test
    fun `given front facing camera, when flipping it, then switch to back camera`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isOnFrontCamera = true)

        sharedCallingViewModel.flipCamera()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.flipToBackCamera(any()) }
        sharedCallingViewModel.callState.isOnFrontCamera shouldBeEqualTo false
    }

    @Test
    fun `given back facing camera, when flipping it, then switch to front camera`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isOnFrontCamera = false)

        sharedCallingViewModel.flipCamera()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.flipToFrontCamera(any()) }
        sharedCallingViewModel.callState.isOnFrontCamera shouldBeEqualTo true
    }

    @Test
    fun `given camera is turned on, when toggling video, then turn off video`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)

        sharedCallingViewModel.toggleVideo()
        advanceUntilIdle()

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
        coVerify(exactly = 1) { arrangement.updateVideoState(any(), VideoState.STOPPED) }
    }

    @Test
    fun `given camera is turned off, when toggling video, then turn on video`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)

        sharedCallingViewModel.toggleVideo()
        advanceUntilIdle()

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo true
        coVerify(exactly = 1) { arrangement.updateVideoState(any(), VideoState.STARTED) }
    }

    @Test
    fun `given an active call, when the user ends call, then invoke hangUpCall useCase`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        sharedCallingViewModel.actions.test {
            sharedCallingViewModel.hangUpCall()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.hangUpCall(any()) }
            assertEquals(SharedCallingViewActions.HungUpCall(conversationId), awaitItem())
        }
    }

    @Test
    fun `given a call, when setVideoPreview is called, then set the video preview`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        sharedCallingViewModel.setVideoPreview(arrangement.view)
        advanceUntilIdle()

        coVerify(exactly = 2) { arrangement.setVideoPreview(any(), any()) }
    }

    @Test
    fun `given a call, when clearVideoPreview is called, then clear view`() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        sharedCallingViewModel.clearVideoPreview()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.setVideoPreview(any(), any()) }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsReceived_ThenNewEmojiIsEmitted() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        sharedCallingViewModel.inCallReactions.test {

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
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        // when
        arrangement.reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍")))

        val recentReaction = sharedCallingViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("👍", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenNewInCallReactionIsReceived_ThenRecentReactionUpdated() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        // when
        arrangement.reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍", "🎉")))

        val recentReaction = sharedCallingViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("🎉", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenReactionMessageIsSent() = runTest(dispatchers.main()) {

        // given
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        // when
        sharedCallingViewModel.onReactionClick("👍")

        // then
        coVerify(exactly = 1) {
            arrangement.sendInCallReactionUseCase(OngoingCallViewModelTest.conversationId, "👍")
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenNewEmojiIsEmitted() = runTest(dispatchers.main()) {

        // given
        val (_, sharedCallingViewModel) = Arrangement()
            .withSendInCallReactionUseCaseReturning(Either.Right(Unit))
            .arrange()

        sharedCallingViewModel.inCallReactions.test {
            // when
            sharedCallingViewModel.onReactionClick("👍")

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
            val (arrangement, sharedCallingViewModel) = Arrangement()
                .withSendInCallReactionUseCaseReturning(Either.Right(Unit))
                .arrange()

            // when
            sharedCallingViewModel.onReactionClick("👌")

            // then
            coVerify(exactly = 1) {
                arrangement.sendInCallReactionUseCase(OngoingCallViewModelTest.conversationId, "👌")
            }
            assertTrue(sharedCallingViewModel.recentReactions.containsValue("👌"))
        }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionSentFails_ThenNoEmojiIsEmitted() = runTest(dispatchers.main()) {
        // given
        val (_, sharedCallingViewModel) = Arrangement()
            .withSendInCallReactionUseCaseReturning(Either.Left(NetworkFailure.NoNetworkConnection(IllegalStateException())))
            .arrange()

        sharedCallingViewModel.inCallReactions.test {
            // when
            sharedCallingViewModel.onReactionClick("👍")

            // then
            expectNoEvents()
        }
    }

    @Test
    fun givenActiveCall_whenCallStateChanges_thenCallAndParticipantStatesAreUpdated() = runTest(dispatchers.main()) {
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        arrangement.callFlow.emit(call.copy(status = CallStatus.ANSWERED, participants = emptyList()))
        advanceUntilIdle()
        sharedCallingViewModel.callState.conversationId shouldBeEqualTo call.conversationId
        sharedCallingViewModel.callState.callStatus shouldBeEqualTo CallStatus.ANSWERED
        sharedCallingViewModel.participantsState shouldBeEqualTo persistentListOf()

        arrangement.callFlow.emit(call.copy(status = CallStatus.ESTABLISHED, participants = listOf(selfParticipant)))
        advanceUntilIdle()
        sharedCallingViewModel.callState.conversationId shouldBeEqualTo call.conversationId
        sharedCallingViewModel.callState.callStatus shouldBeEqualTo CallStatus.ESTABLISHED
        sharedCallingViewModel.participantsState shouldBeEqualTo persistentListOf(
            arrangement.uiCallParticipantMapper.toUICallParticipant(selfParticipant, ClientId(selfParticipant.clientId))
        )
    }

    @Test
    fun `given a call, when rotation changes, then set the new UI rotation`() = runTest(dispatchers.main()) {
        // given
        val (arrangement, sharedCallingViewModel) = Arrangement().arrange()

        // when
        sharedCallingViewModel.setUIRotation(Surface.ROTATION_90)
        advanceUntilIdle()

        // then
        coVerify(exactly = 1) {
            arrangement.setUIRotationUseCase(eq(PlatformRotation(Surface.ROTATION_90)))
        }
    }

    inner class Arrangement {
        @MockK
        lateinit var observeLastActiveCall: ObserveLastActiveCallWithSortedParticipantsUseCase

        @MockK
        lateinit var hangUpCall: HangUpCallUseCase

        @MockK
        lateinit var muteCall: MuteCallUseCase

        @MockK
        lateinit var unMuteCall: UnMuteCallUseCase

        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var setVideoPreview: SetVideoPreviewUseCase

        @MockK
        lateinit var updateVideoState: UpdateVideoStateUseCase

        @MockK
        lateinit var turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase

        @MockK
        lateinit var turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase

        @MockK
        lateinit var flipToBackCamera: FlipToBackCameraUseCase

        @MockK
        lateinit var flipToFrontCamera: FlipToFrontCameraUseCase

        @MockK
        lateinit var observeSpeaker: ObserveSpeakerUseCase

        @MockK
        lateinit var observeInCallReactionsUseCase: ObserveInCallReactionsUseCase

        @MockK
        lateinit var sendInCallReactionUseCase: SendInCallReactionUseCase

        @MockK
        lateinit var getCurrentClientId: ObserveCurrentClientIdUseCase

        @MockK
        lateinit var setUIRotationUseCase: SetUIRotationUseCase

        @MockK
        lateinit var view: View

        @MockK
        lateinit var userTypeMapper: UserTypeMapper

        val uiCallParticipantMapper: UICallParticipantMapper by lazy {
            UICallParticipantMapper(userTypeMapper)
        }

        val reactionsFlow = MutableSharedFlow<InCallReactionMessage>()
        val callFlow = MutableSharedFlow<Call?>()

        init {
            MockKAnnotations.init(this, relaxed = true)
            coEvery { observeLastActiveCall.invoke(any()) } returns callFlow
            coEvery { observeConversationDetails.invoke(any()) } returns emptyFlow()
            coEvery { observeSpeaker.invoke() } returns emptyFlow()
            coEvery { observeInCallReactionsUseCase(any()) } returns reactionsFlow
            coEvery { getCurrentClientId() } returns flowOf(currentClientId)
        }

        fun arrange() = this to SharedCallingViewModel(
            conversationId = conversationId,
            selfUserId = TestUser.SELF_USER_ID,
            conversationDetails = observeConversationDetails,
            observeLastActiveCallWithSortedParticipants = observeLastActiveCall,
            hangUpCall = hangUpCall,
            muteCall = muteCall,
            flipToFrontCamera = flipToFrontCamera,
            flipToBackCamera = flipToBackCamera,
            unMuteCall = unMuteCall,
            setVideoPreview = setVideoPreview,
            setUIRotationUseCase = setUIRotationUseCase,
            updateVideoState = updateVideoState,
            turnLoudSpeakerOff = turnLoudSpeakerOff,
            turnLoudSpeakerOn = turnLoudSpeakerOn,
            observeSpeaker = observeSpeaker,
            uiCallParticipantMapper = uiCallParticipantMapper,
            userTypeMapper = userTypeMapper,
            observeInCallReactionsUseCase = observeInCallReactionsUseCase,
            sendInCallReactionUseCase = sendInCallReactionUseCase,
            getCurrentClientId = getCurrentClientId,
            dispatchers = dispatchers,
        )

        fun withSendInCallReactionUseCaseReturning(result: Either<NetworkFailure, Unit>) = apply {
            coEvery { sendInCallReactionUseCase(conversationId, any()) } returns result
        }
    }

    companion object {
        private val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        private val currentClientId = ClientId("current_client_id")
        private val call = Call(
            conversationId = conversationId,
            status = CallStatus.ESTABLISHED,
            isMuted = true,
            isCameraOn = false,
            isCbrEnabled = false,
            callerId = TestUser.SELF_USER_ID,
            conversationName = "User Name",
            conversationType = Conversation.Type.OneOnOne,
            callerName = "otherUsername",
            callerTeamName = "team_1",
        )
        private val selfParticipant = Participant(
            id = TestUser.SELF_USER_ID,
            clientId = currentClientId.value,
            isMuted = true,
            isCameraOn = false,
            isSharingScreen = false,
            hasEstablishedAudio = true,
            name = "User Name",
            avatarAssetId = null,
            userType = UserType.ADMIN,
            isSpeaking = false,
            accentId = 0,
        )
    }
}
