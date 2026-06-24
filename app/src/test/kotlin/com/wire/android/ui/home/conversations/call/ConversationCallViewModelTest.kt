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
package com.wire.android.ui.home.conversations.call

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.framework.TestConversationDetails
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveConferenceCallingEnabledUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class ConversationCallViewModelTest {

    @Test
    fun `given calling enabled event, when we observe it, then its properly propagated`() = runTest {
        val (_, viewModel) = Arrangement()
            .withConferenceCallingEnabledResponse()
            .arrange()

        val callingEnabled = viewModel.callingEnabled

        callingEnabled.test {
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `given no calling enabled event, when we observe it, then there are no events propagated`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        val callingEnabled = viewModel.callingEnabled

        callingEnabled.test {
            expectNoEvents()
        }
    }

    @Test
    fun `given participants count is observed, when arranging, then update participants count state`() = runTest {
        val (_, viewModel) = Arrangement()
            .withParticipantsCount(LARGE_GROUP_PARTICIPANTS_COUNT)
            .arrange()

        advanceUntilIdle()

        assertEquals(LARGE_GROUP_PARTICIPANTS_COUNT, viewModel.conversationCallViewState.participantsCount)
    }

    @Test
    fun `given cached large participants count, when starting call, then use cached count`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withParticipantsCount(LARGE_GROUP_PARTICIPANTS_COUNT)
            .arrange()

        advanceUntilIdle()

        viewModel.startCallIfPossible(Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrInitiateCallScreenDialogType.CallConfirmation>(viewModel.joinOrInitiateCallViewState.dialogType).also {
            assertEquals(LARGE_GROUP_PARTICIPANTS_COUNT, it.participantsCount)
        }
        coVerify(exactly = 1) { arrangement.observeParticipantsForConversation(arrangement.conversationId) }
    }

    @Test
    fun `given ongoing call in valid conversation, when observing calls, then set ongoing call state`() = runTest {
        val (_, viewModel) = Arrangement()
            .withOngoingCalls(listOf(ongoingCall))
            .withConversationDetails(ObserveConversationDetailsUseCase.Result.Success(TestConversationDetails.GROUP))
            .arrange()

        advanceUntilIdle()

        assertTrue(viewModel.conversationCallViewState.hasOngoingCall)
    }

    @Test
    fun `given ongoing call in another conversation, when observing calls, then keep ongoing call state false`() = runTest {
        val (_, viewModel) = Arrangement()
            .withOngoingCalls(listOf(otherConversationOngoingCall))
            .withConversationDetails(ObserveConversationDetailsUseCase.Result.Success(TestConversationDetails.GROUP))
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.conversationCallViewState.hasOngoingCall)
    }

    @Test
    fun `given ongoing call in conversation where self is not member, when observing calls, then keep ongoing call state false`() = runTest {
        val (_, viewModel) = Arrangement()
            .withOngoingCalls(listOf(ongoingCall))
            .withConversationDetails(
                ObserveConversationDetailsUseCase.Result.Success(
                    TestConversationDetails.GROUP.copy(isSelfUserMember = false)
                )
            )
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.conversationCallViewState.hasOngoingCall)
    }

    @Test
    fun `given ongoing call and failed conversation details, when observing calls, then keep ongoing call state false`() = runTest {
        val (_, viewModel) = Arrangement()
            .withOngoingCalls(listOf(ongoingCall))
            .withConversationDetails(ObserveConversationDetailsUseCase.Result.Failure(StorageFailure.DataNotFound))
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.conversationCallViewState.hasOngoingCall)
    }

    private class Arrangement {
        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var observeOngoingCalls: ObserveOngoingCallsUseCase

        @MockK
        private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var joinCall: AnswerCallUseCase

        @MockK
        lateinit var endCall: EndCallUseCase

        @MockK
        private lateinit var observeSyncState: ObserveSyncStateUseCase

        @MockK
        private lateinit var isConferenceCallingEnabled: IsEligibleToStartCallUseCase

        @MockK
        private lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var observeParticipantsForConversation: ObserveParticipantsForConversationUseCase

        @MockK
        lateinit var setUserInformedAboutVerificationUseCase: SetUserInformedAboutVerificationUseCase

        @MockK
        lateinit var observeDegradedConversationNotifiedUseCase: ObserveDegradedConversationNotifiedUseCase

        @MockK
        lateinit var observeSelfUserUseCase: ObserveSelfUserUseCase

        @MockK
        lateinit var observeConferenceCallingEnabled: ObserveConferenceCallingEnabledUseCase

        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val currentAccount = UserId("current-account-id", "domain.com")

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId = conversationId)
            coEvery { observeEstablishedCalls.invoke() } returns emptyFlow()
            coEvery { observeOngoingCalls.invoke() } returns emptyFlow()
            coEvery { observeConversationDetails(any()) } returns flowOf()
            coEvery { observeParticipantsForConversation(any()) } returns flowOf()
            every { observeSyncState() } returns flowOf(SyncState.Live)
            coEvery { isConferenceCallingEnabled(any(), any()) } returns ConferenceCallingResult.Enabled
            coEvery { setUserInformedAboutVerificationUseCase(any()) } returns Unit
            every { observeDegradedConversationNotifiedUseCase(any()) } returns flowOf(true)
            coEvery { observeSelfUserUseCase() } returns flowOf()
            coEvery { observeConferenceCallingEnabled() } returns flowOf()
        }

        fun withConferenceCallingEnabledResponse() = apply {
            coEvery { observeConferenceCallingEnabled() } returns flowOf(Unit)
        }

        fun withParticipantsCount(participantsCount: Int) = apply {
            coEvery { observeParticipantsForConversation(any()) } returns flowOf(
                ConversationParticipantsData(allParticipantsCount = participantsCount)
            )
        }

        fun withOngoingCalls(calls: List<Call>) = apply {
            coEvery { observeOngoingCalls() } returns flowOf(calls)
        }

        fun withConversationDetails(result: ObserveConversationDetailsUseCase.Result) = apply {
            coEvery { observeConversationDetails(any()) } returns flowOf(result)
        }

        fun arrange(): Pair<Arrangement, ConversationCallViewModel> = this to ConversationCallViewModel(
            savedStateHandle = savedStateHandle,
            observeOngoingCalls = observeOngoingCalls,
            observeEstablishedCalls = observeEstablishedCalls,
            answerCall = joinCall,
            endCall = endCall,
            observeSyncState = observeSyncState,
            isConferenceCallingEnabled = isConferenceCallingEnabled,
            observeConversationDetails = observeConversationDetails,
            observeParticipantsForConversation = observeParticipantsForConversation,
            setUserInformedAboutVerification = setUserInformedAboutVerificationUseCase,
            observeDegradedConversationNotified = observeDegradedConversationNotifiedUseCase,
            observeConferenceCallingEnabled = observeConferenceCallingEnabled,
            observeSelf = observeSelfUserUseCase,
            currentAccount = currentAccount,
        )
    }

    companion object {
        private const val LARGE_GROUP_PARTICIPANTS_COUNT = 6
        private val otherConversationId = ConversationId("other-conversation-id", "some.dummy.domain")
        private val ongoingCall = call()
        private val otherConversationOngoingCall = call(conversationId = otherConversationId)

        private fun call(conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")) = Call(
            conversationId = conversationId,
            status = CallStatus.INCOMING,
            isMuted = false,
            isCameraOn = false,
            isCbrEnabled = false,
            callerId = QualifiedID("caller-id", "some.dummy.domain"),
            conversationName = "conversation",
            conversationType = Conversation.Type.Group.Regular,
            callerName = "caller",
            callerTeamName = "team"
        )
    }
}
