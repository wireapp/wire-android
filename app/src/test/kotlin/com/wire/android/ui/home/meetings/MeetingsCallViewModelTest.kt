/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.meetings

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.call.JoinOrStartCallViewActions
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.meeting.EnsureMeetingIsMLSEstablishedUseCase
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

@ExtendWith(CoroutineTestExtension::class)
class MeetingsCallViewModelTest {

    @Suppress("UnusedFlow")
    @Test
    fun givenMeetingMLSIsEstablished_whenStartingCall_thenEnsureMLSAndInitiateMeetingCall() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withMeetingMLSEstablished()
            .arrange()

        viewModel.callManager.actions.test {
            viewModel.startCallIfPossible(TestConversation.ID)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.InitiatedCall(TestConversation.ID, TestUser.SELF_USER_ID), awaitItem())
            assertFalse(viewModel.notEstablishedDialogState.isVisible)
            coVerify(exactly = 1) { arrangement.ensureMeetingIsMLSEstablished(TestConversation.ID) }
            coVerify(exactly = 1) { arrangement.isConferenceCallingEnabled(TestConversation.ID, Conversation.Type.Group.Meeting) }
            coVerify(exactly = 0) { arrangement.observeConversationMembers(any()) }
        }
    }

    @Test
    fun givenMeetingMLSIsNotEstablished_whenStartingCall_thenShowFailureDialogAndDoNotInitiateCall() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withMeetingMLSNotEstablished()
            .arrange()

        viewModel.callManager.actions.test {
            viewModel.startCallIfPossible(TestConversation.ID)
            advanceUntilIdle()

            expectNoEvents()
            assertTrue(viewModel.notEstablishedDialogState.isVisible)
            coVerify(exactly = 1) { arrangement.ensureMeetingIsMLSEstablished(TestConversation.ID) }
            coVerify(exactly = 0) { arrangement.isConferenceCallingEnabled(any(), any()) }
        }
    }

    @Test
    fun givenMeetingMLSIsEstablished_whenJoiningCall_thenEnsureMLSAndJoinCall() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withMeetingMLSEstablished()
            .arrange()

        viewModel.callManager.actions.test {
            viewModel.joinOngoingCall(TestConversation.ID)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.JoinedCall(TestConversation.ID, TestUser.SELF_USER_ID), awaitItem())
            assertFalse(viewModel.notEstablishedDialogState.isVisible)
            coVerify(exactly = 1) { arrangement.ensureMeetingIsMLSEstablished(TestConversation.ID) }
            coVerify(exactly = 1) { arrangement.answerCall(conversationId = TestConversation.ID) }
        }
    }

    @Test
    fun givenMeetingMLSIsNotEstablished_whenJoiningCall_thenShowFailureDialogAndDoNotJoinCall() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withMeetingMLSNotEstablished()
            .arrange()

        viewModel.callManager.actions.test {
            viewModel.joinOngoingCall(TestConversation.ID)
            advanceUntilIdle()

            expectNoEvents()
            assertTrue(viewModel.notEstablishedDialogState.isVisible)
            coVerify(exactly = 1) { arrangement.ensureMeetingIsMLSEstablished(TestConversation.ID) }
            coVerify(exactly = 0) { arrangement.answerCall(conversationId = any()) }
        }
    }

    inner class Arrangement {
        @MockK
        lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var observeConversationMembers: ObserveConversationMembersUseCase

        @MockK
        lateinit var answerCall: AnswerCallUseCase

        @MockK
        lateinit var endCall: EndCallUseCase

        @MockK
        lateinit var observeSyncState: ObserveSyncStateUseCase

        @MockK
        lateinit var isConferenceCallingEnabled: IsEligibleToStartCallUseCase

        @MockK
        lateinit var setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase

        @MockK
        lateinit var observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase

        @MockK
        lateinit var observeSelfUser: ObserveSelfUserUseCase

        @MockK
        lateinit var ensureMeetingIsMLSEstablished: EnsureMeetingIsMLSEstablishedUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { observeEstablishedCalls() } returns emptyFlow()
            coEvery { observeConversationMembers(any()) } returns flowOf()
            coEvery { answerCall(conversationId = any()) } returns Unit
            coEvery { endCall(any()) } returns Unit
            every { observeSyncState() } returns flowOf(SyncState.Live)
            coEvery { isConferenceCallingEnabled(any(), any()) } returns ConferenceCallingResult.Enabled
            coEvery { setUserInformedAboutVerification(any()) } returns Unit
            every { observeDegradedConversationNotified(any()) } returns flowOf(true)
            coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER.copy(userType = UserTypeInfo.Regular(UserType.GUEST)))
            coEvery { ensureMeetingIsMLSEstablished(any()) } returns true
        }

        fun withMeetingMLSEstablished() = apply {
            coEvery { ensureMeetingIsMLSEstablished(any()) } returns true
        }
        fun withMeetingMLSNotEstablished() = apply {
            coEvery { ensureMeetingIsMLSEstablished(any()) } returns false
        }
        fun arrange() = this to MeetingsCallViewModel(
            currentAccount = TestUser.SELF_USER_ID,
            observeEstablishedCalls = observeEstablishedCalls,
            observeConversationMembers = observeConversationMembers,
            answerCall = answerCall,
            endCall = endCall,
            observeSyncState = observeSyncState,
            isConferenceCallingEnabled = isConferenceCallingEnabled,
            setUserInformedAboutVerification = setUserInformedAboutVerification,
            observeDegradedConversationNotified = observeDegradedConversationNotified,
            observeSelf = observeSelfUser,
            ensureMeetingIsMLSEstablished = ensureMeetingIsMLSEstablished,
        )
    }
}
