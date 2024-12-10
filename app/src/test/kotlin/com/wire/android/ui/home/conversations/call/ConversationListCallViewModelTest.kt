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
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveConferenceCallingEnabledUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class ConversationListCallViewModelTest {

    @Test
    fun `given join dialog displayed, when user dismiss it, then hide it`() = runTest {
        val (_, viewModel) = Arrangement()
            .withDefaultAnswers()
            .arrange()
        viewModel.conversationCallViewState = viewModel.conversationCallViewState.copy(
            shouldShowJoinAnywayDialog = true
        )

        viewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, viewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given no ongoing call, when user tries to join a call, then invoke answerCall call use case`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withDefaultAnswers()
            .withJoinCallResponse()
            .arrange()
        viewModel.conversationCallViewState = viewModel.conversationCallViewState.copy(hasEstablishedCall = false)

        viewModel.joinOngoingCall(arrangement.onAnswered)

        coVerify(exactly = 1) { arrangement.joinCall(conversationId = any()) }
        coVerify(exactly = 1) { arrangement.onAnswered(any()) }
        assertEquals(false, viewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given an ongoing call, when user tries to join a call, then show JoinCallAnywayDialog`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withDefaultAnswers()
            .arrange()
        viewModel.conversationCallViewState = viewModel.conversationCallViewState.copy(hasEstablishedCall = true)

        viewModel.joinOngoingCall(arrangement.onAnswered)

        assertEquals(true, viewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
        coVerify(inverse = true) { arrangement.joinCall(conversationId = any()) }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to join a call, then end current call and join the newer one`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withDefaultAnswers()
            .withEndCallResponse()
            .arrange()
        viewModel.conversationCallViewState =
            viewModel.conversationCallViewState.copy(hasEstablishedCall = true)
        viewModel.establishedCallConversationId = ConversationId("value", "Domain")

        viewModel.joinAnyway(arrangement.onAnswered)

        coVerify(exactly = 1) { arrangement.endCall(any()) }
    }

    @Test
    fun `given self team role as admin in conversation, when we observe own role, then its properly propagated`() = runTest {
        val (_, viewModel) = Arrangement()
            .withDefaultAnswers()
            .withSelfAsAdmin()
            .arrange()

        val role = viewModel.selfTeamRole

        assertEquals(UserType.ADMIN, role.value)
    }

    @Test
    fun `given calling enabled event, when we observe it, then its properly propagated`() = runTest {
        val (_, viewModel) = Arrangement()
            .withDefaultAnswers()
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
            .withDefaultAnswers()
            .arrange()

        val callingEnabled = viewModel.callingEnabled

        callingEnabled.test {
            expectNoEvents()
        }
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

        @MockK(relaxed = true)
        lateinit var onAnswered: (conversationId: ConversationId) -> Unit

        @MockK
        private lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        private lateinit var observeParticipantsForConversation: ObserveParticipantsForConversationUseCase

        @MockK
        lateinit var setUserInformedAboutVerificationUseCase: SetUserInformedAboutVerificationUseCase

        @MockK
        lateinit var observeDegradedConversationNotifiedUseCase: ObserveDegradedConversationNotifiedUseCase

        @MockK
        lateinit var getSelfUserUseCase: GetSelfUserUseCase

        @MockK
        lateinit var observeConferenceCallingEnabled: ObserveConferenceCallingEnabledUseCase

        init {
            MockKAnnotations.init(this)
        }

        suspend fun withDefaultAnswers() = apply {
            val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId = conversationId)
            coEvery { observeEstablishedCalls.invoke() } returns emptyFlow()
            coEvery { observeOngoingCalls.invoke() } returns emptyFlow()
            coEvery { observeConversationDetails(any()) } returns flowOf()
            coEvery { observeParticipantsForConversation(any()) } returns flowOf()
            coEvery { setUserInformedAboutVerificationUseCase(any()) } returns Unit
            coEvery { observeDegradedConversationNotifiedUseCase(any()) } returns flowOf(false)
            coEvery { getSelfUserUseCase() } returns flowOf()
            coEvery { observeConferenceCallingEnabled() } returns flowOf()
        }

        suspend fun withSelfAsAdmin() = apply {
            coEvery { getSelfUserUseCase.invoke() } returns flowOf(TestUser.SELF_USER.copy(userType = UserType.ADMIN))
        }

        suspend fun withConferenceCallingEnabledResponse() = apply {
            coEvery { observeConferenceCallingEnabled() } returns flowOf(Unit)
        }

        suspend fun withJoinCallResponse() = apply {
            coEvery { joinCall(conversationId = any()) } returns Unit
        }

        suspend fun withEndCallResponse() = apply {
            coEvery { endCall(any()) } returns Unit
        }

        fun arrange(): Pair<Arrangement, ConversationListCallViewModel> = this to ConversationListCallViewModel(
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
            getSelf = getSelfUserUseCase
        )
    }
}
