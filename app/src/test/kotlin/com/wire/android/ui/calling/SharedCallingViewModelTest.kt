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
import com.wire.android.ui.calling.model.ReactionSender
import com.wire.android.ui.calling.usecase.HangUpCallUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.InCallReactionMessage
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class SharedCallingViewModelTest {

    @MockK
    private lateinit var observeEstablishedCall: ObserveEstablishedCallWithSortedParticipantsUseCase

    @MockK
    private lateinit var hangUpCall: HangUpCallUseCase

    @MockK
    private lateinit var muteCall: MuteCallUseCase

    @MockK
    private lateinit var unMuteCall: UnMuteCallUseCase

    @MockK
    private lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    private lateinit var setVideoPreview: SetVideoPreviewUseCase

    @MockK
    private lateinit var updateVideoState: UpdateVideoStateUseCase

    @MockK
    private lateinit var turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase

    @MockK
    private lateinit var turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase

    @MockK
    private lateinit var flipToBackCamera: FlipToBackCameraUseCase

    @MockK
    private lateinit var flipToFrontCamera: FlipToFrontCameraUseCase

    @MockK
    private lateinit var observeSpeaker: ObserveSpeakerUseCase

    @MockK
    lateinit var observeInCallReactionsUseCase: ObserveInCallReactionsUseCase

    @MockK
    lateinit var sendInCallReactionUseCase: SendInCallReactionUseCase

    @MockK
    lateinit var getCurrentClientId: ObserveCurrentClientIdUseCase

    @MockK
    private lateinit var view: View

    @MockK
    private lateinit var userTypeMapper: UserTypeMapper

    private val uiCallParticipantMapper: UICallParticipantMapper by lazy {
        UICallParticipantMapper(userTypeMapper)
    }

    private val dispatcherProvider = TestDispatcherProvider()

    private lateinit var sharedCallingViewModel: SharedCallingViewModel

    val reactionsFlow = MutableSharedFlow<InCallReactionMessage>()
    val callFlow = MutableSharedFlow<Call?>()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { observeEstablishedCall.invoke() } returns callFlow
        coEvery { observeConversationDetails.invoke(any()) } returns emptyFlow()
        coEvery { observeSpeaker.invoke() } returns emptyFlow()
        coEvery { observeInCallReactionsUseCase(any()) } returns reactionsFlow
        coEvery { getCurrentClientId() } returns flowOf(ClientId("clientId"))

        sharedCallingViewModel = SharedCallingViewModel(
            conversationId = conversationId,
<<<<<<< HEAD
            selfUserId = TestUser.SELF_USER_ID,
=======
            inCallReactionsEnabled = true,
>>>>>>> 0f12ced1a (feat: add compile time flag to enable/disable in call reactions [WPB-18894] (#4138))
            conversationDetails = observeConversationDetails,
            observeEstablishedCallWithSortedParticipants = observeEstablishedCall,
            hangUpCall = hangUpCall,
            muteCall = muteCall,
            flipToFrontCamera = flipToFrontCamera,
            flipToBackCamera = flipToBackCamera,
            unMuteCall = unMuteCall,
            setVideoPreview = setVideoPreview,
            updateVideoState = updateVideoState,
            turnLoudSpeakerOff = turnLoudSpeakerOff,
            turnLoudSpeakerOn = turnLoudSpeakerOn,
            observeSpeaker = observeSpeaker,
            uiCallParticipantMapper = uiCallParticipantMapper,
            userTypeMapper = userTypeMapper,
            observeInCallReactionsUseCase = observeInCallReactionsUseCase,
            sendInCallReactionUseCase = sendInCallReactionUseCase,
            getCurrentClientId = getCurrentClientId,
            dispatchers = dispatcherProvider
        )
    }

    @Test
    fun `given isMuted value is null, when toggling microphone, then do not update microphone state`() =
        runTest(dispatcherProvider.main()) {
            sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = null)
            coEvery { muteCall(conversationId) } returns Unit

            sharedCallingViewModel.toggleMute()

            sharedCallingViewModel.callState.isMuted shouldBeEqualTo null
        }

    @Test
    fun `given an un-muted call, when toggling microphone, then mute the call`() = runTest(dispatcherProvider.main()) {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)
        coEvery { muteCall(conversationId) } returns Unit

        sharedCallingViewModel.toggleMute()
        advanceUntilIdle()

        coVerify(exactly = 1) { muteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given a muted call, when toggling microphone, then un-mute the call`() = runTest(dispatcherProvider.main()) {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)
        coEvery { unMuteCall(any()) } returns Unit

        sharedCallingViewModel.toggleMute()
        advanceUntilIdle()

        coVerify(exactly = 1) { unMuteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
    }

    @Test
    fun `given user on a preview screen, when muting microphone, then mute the call with false param`() =
        runTest(dispatcherProvider.main()) {
            sharedCallingViewModel.callState =
                sharedCallingViewModel.callState.copy(isMuted = false)
            coEvery { muteCall(conversationId, false) } returns Unit

            sharedCallingViewModel.toggleMute(true)
            advanceUntilIdle()

            coVerify(exactly = 1) { muteCall(any(), false) }
            sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
        }

    @Test
    fun `given user on a preview screen, when un-muting microphone, then un-mute the call with false param`() =
        runTest(dispatcherProvider.main()) {
            sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)
            coEvery { unMuteCall(conversationId, false) } returns Unit

            sharedCallingViewModel.toggleMute(true)
            advanceUntilIdle()

            coVerify(exactly = 1) { unMuteCall(any(), false) }
            sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
        }

    @Test
    fun `given front facing camera, when flipping it, then switch to back camera`() = runTest(dispatcherProvider.main()) {
        sharedCallingViewModel.callState =
            sharedCallingViewModel.callState.copy(isOnFrontCamera = true)
        coEvery { flipToBackCamera(conversationId) } returns Unit

        sharedCallingViewModel.flipCamera()
        advanceUntilIdle()

        coVerify(exactly = 1) { flipToBackCamera(any()) }
        sharedCallingViewModel.callState.isOnFrontCamera shouldBeEqualTo false
    }

    @Test
    fun `given back facing camera, when flipping it, then switch to front camera`() = runTest(dispatcherProvider.main()) {
        sharedCallingViewModel.callState =
            sharedCallingViewModel.callState.copy(isOnFrontCamera = false)
        coEvery { flipToFrontCamera(conversationId) } returns Unit

        sharedCallingViewModel.flipCamera()
        advanceUntilIdle()

        coVerify(exactly = 1) { flipToFrontCamera(any()) }
        sharedCallingViewModel.callState.isOnFrontCamera shouldBeEqualTo true
    }

    @Test
    fun `given camera is turned on, when toggling video, then turn off video`() = runTest(dispatcherProvider.main()) {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { updateVideoState(any(), any()) } returns Unit

        sharedCallingViewModel.toggleVideo()
        advanceUntilIdle()

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STOPPED) }
    }

    @Test
    fun `given camera is turned off, when toggling video, then turn on video`() = runTest(dispatcherProvider.main()) {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { updateVideoState(any(), any()) } returns Unit

        sharedCallingViewModel.toggleVideo()
        advanceUntilIdle()

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo true
        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STARTED) }
    }

    @Test
    fun `given an active call, when the user ends call, then invoke hangUpCall useCase`() = runTest(dispatcherProvider.main()) {
        coEvery { hangUpCall(any()) } returns Unit

        sharedCallingViewModel.actions.test {
            sharedCallingViewModel.hangUpCall()
            advanceUntilIdle()

            coVerify(exactly = 1) { hangUpCall(any()) }
            assertEquals(SharedCallingViewActions.HungUpCall(conversationId), awaitItem())
        }
    }

    @Test
    fun `given a call, when setVideoPreview is called, then set the video preview`() =
        runTest(dispatcherProvider.main()) {
            coEvery { setVideoPreview(any(), any()) } returns Unit

            sharedCallingViewModel.setVideoPreview(view)
            advanceUntilIdle()

            coVerify(exactly = 2) { setVideoPreview(any(), any()) }
        }

    @Test
    fun `given a call, when clearVideoPreview is called, then clear view`() = runTest(dispatcherProvider.main()) {
        coEvery { setVideoPreview(any(), any()) } returns Unit

        sharedCallingViewModel.clearVideoPreview()
        advanceUntilIdle()

        coVerify(exactly = 1) { setVideoPreview(any(), any()) }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsReceived_ThenNewEmojiIsEmitted() = runTest(dispatcherProvider.main()) {

        sharedCallingViewModel.inCallReactions.test {

            // when
            reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍", "🎉")))

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
    fun givenAnOngoingCall_WhenInCallReactionIsReceived_ThenNewRecentReactionEmitted() = runTest(dispatcherProvider.main()) {

        // when
        reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍")))

        val recentReaction = sharedCallingViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("👍", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenNewInCallReactionIsReceived_ThenRecentReactionUpdated() = runTest(dispatcherProvider.main()) {

        // when
        reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍", "🎉")))

        val recentReaction = sharedCallingViewModel.recentReactions.getValue(TestUser.USER_ID)

        // then
        assertEquals("🎉", recentReaction)
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenReactionMessageIsSent() = runTest(dispatcherProvider.main()) {

        // given
        coEvery { sendInCallReactionUseCase(conversationId, any()) } returns Either.Right(Unit)

        // when
        sharedCallingViewModel.onReactionClick("👍")

        // then
        coVerify(exactly = 1) {
            sendInCallReactionUseCase(OngoingCallViewModelTest.conversationId, "👍")
        }
    }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionIsSent_ThenNewEmojiIsEmitted() = runTest(dispatcherProvider.main()) {

        // given
        coEvery { sendInCallReactionUseCase(conversationId, any()) } returns Either.Right(Unit)

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
        runTest(dispatcherProvider.main()) {

            // given
            coEvery { sendInCallReactionUseCase(conversationId, any()) } returns Either.Right(Unit)

            // when
            sharedCallingViewModel.onReactionClick("👌")

            // then
            coVerify(exactly = 1) {
                sendInCallReactionUseCase(OngoingCallViewModelTest.conversationId, "👌")
            }
            assertTrue(sharedCallingViewModel.recentReactions.containsValue("👌"))
        }

    @Test
    fun givenAnOngoingCall_WhenInCallReactionSentFails_ThenNoEmojiIsEmitted() = runTest(dispatcherProvider.main()) {
        // given
        coEvery { sendInCallReactionUseCase(conversationId, any()) } returns
                Either.Left(NetworkFailure.NoNetworkConnection(IllegalStateException()))

        sharedCallingViewModel.inCallReactions.test {
            // when
            sharedCallingViewModel.onReactionClick("👍")

            // then
            expectNoEvents()
        }
    }

    @Test
    fun givenInCallReactionsDisabled_WhenInCallReactionIsReceived_ThenNoEmojiIsEmitted() = runTest(dispatcherProvider.main()) {
        // given
        val viewModel = SharedCallingViewModel(
            conversationId = conversationId,
            inCallReactionsEnabled = false,
            conversationDetails = observeConversationDetails,
            observeEstablishedCallWithSortedParticipants = observeEstablishedCall,
            endCall = endCall,
            muteCall = muteCall,
            flipToFrontCamera = flipToFrontCamera,
            flipToBackCamera = flipToBackCamera,
            unMuteCall = unMuteCall,
            setVideoPreview = setVideoPreview,
            updateVideoState = updateVideoState,
            turnLoudSpeakerOff = turnLoudSpeakerOff,
            turnLoudSpeakerOn = turnLoudSpeakerOn,
            observeSpeaker = observeSpeaker,
            callRinger = callRinger,
            uiCallParticipantMapper = uiCallParticipantMapper,
            userTypeMapper = userTypeMapper,
            observeInCallReactionsUseCase = observeInCallReactionsUseCase,
            sendInCallReactionUseCase = sendInCallReactionUseCase,
            getCurrentClientId = getCurrentClientId,
            dispatchers = dispatcherProvider
        )

        viewModel.inCallReactions.test {
            // when
            reactionsFlow.emit(InCallReactionMessage(conversationId, TestUser.USER_ID, setOf("👍")))

            // then
            expectNoEvents()
        }
    }

    @Test
    fun givenInCallReactionsDisabled_WhenReactionIsClicked_ThenNoReactionIsSent() = runTest(dispatcherProvider.main()) {
        // given
        val viewModel = SharedCallingViewModel(
            conversationId = conversationId,
            inCallReactionsEnabled = false,
            conversationDetails = observeConversationDetails,
            observeEstablishedCallWithSortedParticipants = observeEstablishedCall,
            endCall = endCall,
            muteCall = muteCall,
            flipToFrontCamera = flipToFrontCamera,
            flipToBackCamera = flipToBackCamera,
            unMuteCall = unMuteCall,
            setVideoPreview = setVideoPreview,
            updateVideoState = updateVideoState,
            turnLoudSpeakerOff = turnLoudSpeakerOff,
            turnLoudSpeakerOn = turnLoudSpeakerOn,
            observeSpeaker = observeSpeaker,
            callRinger = callRinger,
            uiCallParticipantMapper = uiCallParticipantMapper,
            userTypeMapper = userTypeMapper,
            observeInCallReactionsUseCase = observeInCallReactionsUseCase,
            sendInCallReactionUseCase = sendInCallReactionUseCase,
            getCurrentClientId = getCurrentClientId,
            dispatchers = dispatcherProvider
        )

        // when
        viewModel.onReactionClick("👍")

        // then
        coVerify(exactly = 0) { sendInCallReactionUseCase(any(), any()) }
    }

    companion object {
        private val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
    }
}
