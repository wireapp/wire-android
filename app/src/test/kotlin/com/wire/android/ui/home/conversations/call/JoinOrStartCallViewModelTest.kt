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
package com.wire.android.ui.home.conversations.call

import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Suppress("UnusedFlow")
@ExtendWith(CoroutineTestExtension::class)
class JoinOrStartCallViewModelTest {

    @Test
    fun `given JoinAnyway dialog is displayed, when user dismiss it, then hide it`() = runTest {
        val (_, viewModel) = Arrangement()
            .withInitialDialogType(JoinOrStartCallScreenDialogType.JoinAnyway(conversationId))
            .arrange()

        viewModel.dismissDialog()

        assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
    }

    @Test
    fun `given self team role as admin in conversation, when we observe own role, then its properly propagated`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSelfAsAdmin()
            .arrange()

        assertTrue(viewModel.selfTeamRole.value.isTeamAdmin())
    }

    @Test
    fun `given no established call, when joining ongoing call, then join call`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.actions.test {
            viewModel.joinOngoingCall(conversationId)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.answerCall(conversationId = conversationId) }
            assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
            assertEquals(JoinOrStartCallViewActions.JoinedCall(conversationId, currentAccount), awaitItem())
        }
    }

    @Test
    fun `given established call, when joining ongoing call, then show JoinAnyway dialog`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withObserveEstablishedCallsFlow(flowOf(listOf(establishedCall)))
            .arrange()

        viewModel.joinOngoingCall(conversationId)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.JoinAnyway>(viewModel.joinOrStartCallViewState.dialogType).also {
            assertEquals(conversationId, it.conversationId)
        }
        coVerify(exactly = 0) { arrangement.answerCall(conversationId = any()) }
    }

    @Test
    fun `given established call, when joining anyway another call, then end current call and join new call`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withObserveEstablishedCallsFlow(flowOf(listOf(establishedCall)))
            .arrange()

        viewModel.actions.test {
            viewModel.joinAnyway(conversationId)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.endCall(establishedConversationId) }
            coVerify(exactly = 1) { arrangement.answerCall(conversationId = conversationId) }
            assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
            assertEquals(JoinOrStartCallViewActions.JoinedCall(conversationId, currentAccount), awaitItem())
        }
    }

    @Test
    fun `given established call, when initiating another call, then end current call and initiate new call`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withObserveEstablishedCallsFlow(flowOf(listOf(establishedCall)))
            .arrange()

        viewModel.actions.test {
            viewModel.initiateCall(conversationId)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.InitiatedCall(conversationId, currentAccount), awaitItem())
            coVerify(exactly = 1) { arrangement.endCall(establishedConversationId) }
        }
    }

    @Test
    fun `given established call observer emits empty calls, when joining anyway, then do not end stale call`() = runTest {
        val establishedCalls = MutableSharedFlow<List<Call>>(replay = 1)
        val (arrangement, viewModel) = Arrangement()
            .withObserveEstablishedCallsFlow(establishedCalls)
            .arrange()

        establishedCalls.emit(listOf(establishedCall))
        advanceUntilIdle()

        assertTrue(viewModel.joinOrStartCallViewState.hasEstablishedCall)

        establishedCalls.emit(emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.joinOrStartCallViewState.hasEstablishedCall)
        viewModel.actions.test {
            viewModel.joinAnyway(conversationId)
            advanceUntilIdle()

            coVerify(exactly = 0) { arrangement.endCall(establishedConversationId) }
            coVerify(exactly = 1) { arrangement.answerCall(conversationId = conversationId) }
            assertEquals(JoinOrStartCallViewActions.JoinedCall(conversationId, currentAccount), awaitItem())
        }
    }

    @Test
    fun `given no connectivity, when starting call, then show NoConnectivity dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSyncState(SyncState.Waiting)
            .arrange()

        viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.NoConnectivity>(viewModel.joinOrStartCallViewState.dialogType)
    }

    @Test
    fun `given degraded verification not acknowledged, when starting call, then show VerificationDegraded dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withDegradedConversationNotified(false)
            .arrange()

        viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.VerificationDegraded>(viewModel.joinOrStartCallViewState.dialogType)
    }

    @Test
    fun `given degraded verification acknowledged, when starting call, then initiate call immediately`() = runTest {
        val (_, viewModel) = Arrangement()
            .withDegradedConversationNotified(true)
            .arrange()

        viewModel.actions.test {
            viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.InitiatedCall(conversationId, currentAccount), awaitItem())
            assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
        }
    }

    @Test
    fun `given degraded verification confirmed, when starting call, then mark user informed and initiate call`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withDegradedConversationNotified(false)
            .arrange()

        viewModel.actions.test {
            viewModel.startCallAfterDegradedVerification(conversationId, Conversation.Type.Group.Regular)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.InitiatedCall(conversationId, currentAccount), awaitItem())
            coVerify(exactly = 1) { arrangement.setUserInformedAboutVerification(conversationId) }
            coVerify(exactly = 0) { arrangement.observeDegradedConversationNotified(any()) }
            assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
        }
    }

    @Test
    fun `given enabled call with small group, when starting call, then initiate call immediately`() = runTest {
        val (_, viewModel) = Arrangement()
            .withParticipantsCount(SMALL_GROUP_PARTICIPANTS_COUNT)
            .arrange()

        viewModel.actions.test {
            viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.InitiatedCall(conversationId, currentAccount), awaitItem())
            assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
        }
    }

    @Test
    fun `given enabled call with large group, when starting call, then show CallConfirmation dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withParticipantsCount(LARGE_GROUP_PARTICIPANTS_COUNT)
            .arrange()

        viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.CallConfirmation>(viewModel.joinOrStartCallViewState.dialogType).also {
            assertEquals(conversationId, it.conversationId)
            assertEquals(LARGE_GROUP_PARTICIPANTS_COUNT, it.participantsCount)
            assertEquals(Conversation.Type.Group.Regular, it.conversationType)
        }
    }

    @Test
    fun `given call with large group confirmed, when starting call, then initiate call`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.actions.test {
            viewModel.startCallAfterConfirming(conversationId, Conversation.Type.Group.Regular)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.InitiatedCall(conversationId, currentAccount), awaitItem())
            coVerify(exactly = 0) { arrangement.observeParticipantsForConversation(any()) }
            assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
        }
    }

    @Test
    fun `given calling disabled as call is established, when starting call, then open already established call`() = runTest {
        val (_, viewModel) = Arrangement()
            .withIsConferenceCallingEnabled(ConferenceCallingResult.Disabled.Established)
            .arrange()

        viewModel.actions.test {
            viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
            advanceUntilIdle()

            assertEquals(JoinOrStartCallViewActions.JoinedCall(conversationId, currentAccount), awaitItem())
            assertIs<JoinOrStartCallScreenDialogType.None>(viewModel.joinOrStartCallViewState.dialogType)
        }
    }

    @Test
    fun `given calling disabled as another call is established, when starting call, then show ongoing active call dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withIsConferenceCallingEnabled(ConferenceCallingResult.Disabled.OngoingCall)
            .arrange()

        viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.OngoingActiveCall>(viewModel.joinOrStartCallViewState.dialogType).also {
            assertEquals(conversationId, it.conversationId)
        }
    }

    @Test
    fun `given unavailable call for internal user, when starting call, then show team member unavailable dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSelfUserType(UserType.INTERNAL)
            .withIsConferenceCallingEnabled(ConferenceCallingResult.Disabled.Unavailable)
            .arrange()

        viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.TeamMember>(viewModel.joinOrStartCallViewState.dialogType)
    }

    @Test
    fun `given unavailable call for admin, when starting call, then show team admin unavailable dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSelfUserType(UserType.ADMIN)
            .withIsConferenceCallingEnabled(ConferenceCallingResult.Disabled.Unavailable)
            .arrange()

        viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.TeamAdmin>(viewModel.joinOrStartCallViewState.dialogType)
    }

    @Test
    fun `given unavailable call for guest, when starting call, then show generic unavailable dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSelfUserType(UserType.GUEST)
            .withIsConferenceCallingEnabled(ConferenceCallingResult.Disabled.Unavailable)
            .arrange()

        viewModel.startCallIfPossible(conversationId, Conversation.Type.Group.Regular)
        advanceUntilIdle()

        assertIs<JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.Other>(viewModel.joinOrStartCallViewState.dialogType)
    }

    private class Arrangement {
        @MockK
        lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var observeParticipantsForConversation: ObserveParticipantsForConversationUseCase

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
        lateinit var observeSelfUserUseCase: ObserveSelfUserUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeEstablishedCalls() } returns emptyFlow()
            coEvery { observeParticipantsForConversation(any()) } returns flowOf(ConversationParticipantsData())
            coEvery { answerCall(conversationId = any()) } returns Unit
            coEvery { endCall(any()) } returns Unit
            every { observeSyncState() } returns flowOf(SyncState.Live)
            coEvery { isConferenceCallingEnabled(any(), any()) } returns ConferenceCallingResult.Enabled
            coEvery { setUserInformedAboutVerification(any()) } returns Unit
            every { observeDegradedConversationNotified(any()) } returns flowOf(true)
            coEvery { observeSelfUserUseCase() } returns flowOf(
                TestUser.SELF_USER.copy(userType = UserTypeInfo.Regular(UserType.GUEST))
            )
        }

        fun withSyncState(syncState: SyncState) = apply {
            every { observeSyncState() } returns flowOf(syncState)
        }

        fun withDegradedConversationNotified(notified: Boolean) = apply {
            every { observeDegradedConversationNotified(any()) } returns flowOf(notified)
        }

        fun withParticipantsCount(participantsCount: Int) = apply {
            coEvery { observeParticipantsForConversation(any()) } returns flowOf(
                ConversationParticipantsData(allParticipantsCount = participantsCount)
            )
        }

        fun withIsConferenceCallingEnabled(result: ConferenceCallingResult) = apply {
            coEvery { isConferenceCallingEnabled(any(), any()) } returns result
        }

        fun withSelfUserType(userType: UserType) = apply {
            coEvery { observeSelfUserUseCase() } returns flowOf(
                TestUser.SELF_USER.copy(userType = UserTypeInfo.Regular(userType))
            )
        }

        fun withSelfAsAdmin() = apply {
            coEvery { observeSelfUserUseCase.invoke() } returns flowOf(
                TestUser.SELF_USER.copy(
                    userType = UserTypeInfo.Regular(UserType.ADMIN)
                )
            )
        }

        fun withObserveEstablishedCallsFlow(calls: Flow<List<Call>>) = apply {
            coEvery { observeEstablishedCalls() } returns calls
        }

        fun withInitialDialogType(dialogType: JoinOrStartCallScreenDialogType) = apply {
            this.initialDialogType = dialogType
        }

        private var initialDialogType: JoinOrStartCallScreenDialogType = JoinOrStartCallScreenDialogType.None

        fun arrange() = this to JoinOrStartCallViewModel(
            currentAccount = currentAccount,
            observeEstablishedCalls = observeEstablishedCalls,
            observeParticipantsForConversation = observeParticipantsForConversation,
            answerCall = answerCall,
            endCall = endCall,
            observeSyncState = observeSyncState,
            isEligibleToStartCall = isConferenceCallingEnabled,
            setUserInformedAboutVerification = setUserInformedAboutVerification,
            observeDegradedConversationNotified = observeDegradedConversationNotified,
            observeSelf = observeSelfUserUseCase,
            initialState = JoinOrStartCallViewState(dialogType = initialDialogType)
        )
    }

    companion object {
        private val conversationId = ConversationId("conversation-id", "domain.com")
        private val establishedConversationId = ConversationId("established-conversation-id", "domain.com")
        private val currentAccount = UserId("current-account-id", "domain.com")
        private val establishedCall = Call(
            conversationId = establishedConversationId,
            status = CallStatus.ESTABLISHED,
            isMuted = false,
            isCameraOn = true,
            isCbrEnabled = false,
            callerId = QualifiedID("some_id", "some_domain"),
            conversationName = "some_name",
            conversationType = Conversation.Type.Group.Regular,
            callerName = "some_name",
            callerTeamName = "some_team_name"
        )
        private const val SMALL_GROUP_PARTICIPANTS_COUNT = 3
        private const val LARGE_GROUP_PARTICIPANTS_COUNT = 6
    }
}
