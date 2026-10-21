/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.framework.TestUser
import com.wire.android.session.FakeSessionAdmission
import com.wire.android.ui.analytics.IsAnalyticsAvailableUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.call.RecentlyEndedCallMetadata
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.ObserveRecentlyEndedCallMetadataUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.ShouldAskCallFeedbackUseCaseResult
import com.wire.kalium.logic.feature.user.UpdateNextTimeCallFeedbackUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.impl.annotations.MockK
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class CallFeedbackViewModelTest {

    @Test
    fun `given analytics is not available when use case is observed then it should skip sending feedback event`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        coEvery { arrangement.isAnalyticsAvailable(any(), any()) } returns false

        viewModel.showCallFeedbackFlow.test {
            expectNoEvents()
        }
    }

    @Test
    fun `given short call when use case is observed then send TooShort event`() = runTest {
        val shortDuration = 2L
        val (arrangement, _) = Arrangement()
            .withObserveAskCallFeedbackUseCaseReturning(
                ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.CallDurationIsLessThanOneMinute(
                    shortDuration
                )
            )
            .arrange()

        coVerify(exactly = 1) {
            arrangement.analyticsManager.sendEvent(
                AnalyticsEvent.CallQualityFeedback.TooShort(
                    callDuration = shortDuration.toInt(),
                    isTeamMember = recentlyEndedCallMetadata.isTeamMember,
                    participantsCount = recentlyEndedCallMetadata.callDetails.callParticipantsCount,
                    isScreenSharedDuringCall = recentlyEndedCallMetadata.callDetails.isCallScreenShare,
                    isCameraEnabledDuringCall = recentlyEndedCallMetadata.callDetails.callVideoEnabled
                )
            )
        }
    }

    @Test
    fun `given NextTimeForCallFeedbackIsNotReached when use case is observed then send Muted event`() = runTest {
        val (arrangement, _) = Arrangement()
            .withObserveAskCallFeedbackUseCaseReturning(
                ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.NextTimeForCallFeedbackIsNotReached(
                    CALL_DURATION
                )
            )
            .arrange()

        coVerify(exactly = 1) {
            arrangement.analyticsManager.sendEvent(
                AnalyticsEvent.CallQualityFeedback.Muted(
                    callDuration = CALL_DURATION.toInt(),
                    isTeamMember = recentlyEndedCallMetadata.isTeamMember,
                    participantsCount = recentlyEndedCallMetadata.callDetails.callParticipantsCount,
                    isScreenSharedDuringCall = recentlyEndedCallMetadata.callDetails.isCallScreenShare,
                    isCameraEnabledDuringCall = recentlyEndedCallMetadata.callDetails.callVideoEnabled
                )
            )
        }
    }

    @Test
    fun `given A rate Call is displayed when sending score then invoke event for score with value`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        viewModel.rateCall(5, false)

        coVerify(exactly = 1) {
            arrangement.analyticsManager.sendEvent(
                AnalyticsEvent.CallQualityFeedback.Answered(
                    score = 5,
                    callDuration = recentlyEndedCallMetadata.callDetails.callDurationInSeconds.toInt(),
                    isTeamMember = recentlyEndedCallMetadata.isTeamMember,
                    participantsCount = recentlyEndedCallMetadata.callDetails.callParticipantsCount,
                    isScreenSharedDuringCall = recentlyEndedCallMetadata.callDetails.isCallScreenShare,
                    isCameraEnabledDuringCall = recentlyEndedCallMetadata.callDetails.callVideoEnabled
                )
            )
        }
    }

    @Test
    fun `given a rate call is displayed when dismissing it then invoke event for dismiss`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        viewModel.skipCallFeedback(false)

        coVerify(exactly = 1) {
            arrangement.analyticsManager.sendEvent(
                AnalyticsEvent.CallQualityFeedback.Dismissed(
                    callDuration = recentlyEndedCallMetadata.callDetails.callDurationInSeconds.toInt(),
                    isTeamMember = recentlyEndedCallMetadata.isTeamMember,
                    participantsCount = recentlyEndedCallMetadata.callDetails.callParticipantsCount,
                    isScreenSharedDuringCall = recentlyEndedCallMetadata.callDetails.isCallScreenShare,
                    isCameraEnabledDuringCall = recentlyEndedCallMetadata.callDetails.callVideoEnabled
                )
            )
        }
    }

    @Test
    fun `pending admission and equal account replacement keep one cancellable feedback chain`() = runTest {
        val current = MutableStateFlow<CurrentSessionResult>(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
        val firstFeedback = MutableSharedFlow<ShouldAskCallFeedbackUseCaseResult>()
        val nextFeedback = MutableSharedFlow<ShouldAskCallFeedbackUseCaseResult>()
        val arrangement = Arrangement().withFeedbackFlow(firstFeedback).apply { rawSessions = current }
        val replacement = Arrangement().withFeedbackFlow(nextFeedback)
        arrangement.admission.publish(TestUser.USER_ID, null)
        val (_, viewModel) = arrangement.arrange()
        viewModel.showCallFeedbackFlow.test {
            runCurrent()
            assertEquals(0, firstFeedback.subscriptionCount.value)
            arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
            runCurrent()
            assertEquals(1, firstFeedback.subscriptionCount.value)
            arrangement.admission.publish(TestUser.USER_ID, null)
            runCurrent()
            assertEquals(0, firstFeedback.subscriptionCount.value)
            assertTrue(arrangement.session in arrangement.admission.drained)
            arrangement.admission.publish(TestUser.USER_ID, replacement.session)
            runCurrent()
            assertEquals(1, nextFeedback.subscriptionCount.value)
            nextFeedback.emit(ShouldAskCallFeedbackUseCaseResult.ShouldAskCallFeedback(CALL_DURATION))
            assertEquals(Unit, awaitItem())
            current.value = CurrentSessionResult.Success(AccountInfo.Invalid(TestUser.USER_ID, LogoutReason.SELF_SOFT_LOGOUT))
            runCurrent()
            assertEquals(0, nextFeedback.subscriptionCount.value)
            firstFeedback.emit(ShouldAskCallFeedbackUseCaseResult.ShouldAskCallFeedback(CALL_DURATION))
            nextFeedback.emit(ShouldAskCallFeedbackUseCaseResult.ShouldAskCallFeedback(CALL_DURATION))
            runCurrent()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.rateCall(5, false)
        viewModel.skipCallFeedback(false)
        runCurrent()
        coVerify(exactly = 0) { arrangement.updateNextTimeCallFeedback(any()) }
        coVerify(exactly = 0) { replacement.updateNextTimeCallFeedback(any()) }
        coVerify(exactly = 0) { arrangement.observeRecentlyEndedCallMetadata() }
        coVerify(exactly = 0) { replacement.observeRecentlyEndedCallMetadata() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `pending A cannot block a qualified B using captured analytics and recent call metadata`() = runTest {
        val otherId = TestUser.USER_ID.copy(domain = "other.example")
        val current = MutableStateFlow<CurrentSessionResult>(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
        val shortCall = ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.CallDurationIsLessThanOneMinute(2L)
        val otherMetadata = recentlyEndedCallMetadata.copy(isTeamMember = false)
        val replacement = Arrangement().withObserveAskCallFeedbackUseCaseReturning(shortCall).withMetadata(otherMetadata)
        val arrangement = Arrangement().apply { rawSessions = current }
        arrangement.admission.publish(TestUser.USER_ID, null)
        val (_, viewModel) = arrangement.arrange()
        arrangement.admission.publish(otherId, replacement.session)
        current.value = CurrentSessionResult.Success(AccountInfo.Valid(otherId))
        runCurrent()
        coVerify(exactly = 1) { arrangement.isAnalyticsAvailable(otherId, replacement.session) }
        coVerify(exactly = 1) {
            arrangement.analyticsManager.sendEvent(
                AnalyticsEvent.CallQualityFeedback.TooShort(
                    callDuration = 2,
                    isTeamMember = false,
                    participantsCount = otherMetadata.callDetails.callParticipantsCount,
                    isScreenSharedDuringCall = otherMetadata.callDetails.isCallScreenShare,
                    isCameraEnabledDuringCall = otherMetadata.callDetails.callVideoEnabled
                )
            )
        }
        coVerify(exactly = 0) { arrangement.session.observeSyncState() }
        coVerify(exactly = 0) { arrangement.observeRecentlyEndedCallMetadata() }
        coVerify(exactly = 0) { arrangement.isAnalyticsAvailable(any()) }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `revoked rate request drains its metadata read and cannot replay on replacement`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val (arrangement, viewModel) = Arrangement().arrange()
        coEvery { arrangement.observeRecentlyEndedCallMetadata() } returns flow {
            entered.complete(Unit)
            try { awaitCancellation() } finally { drained.complete(Unit) }
        }
        viewModel.rateCall(5, false)
        entered.await()
        arrangement.admission.publish(TestUser.USER_ID, null)
        runCurrent()
        assertTrue(drained.isCompleted)
        val replacement = Arrangement()
        arrangement.admission.publish(TestUser.USER_ID, replacement.session)
        runCurrent()
        coVerify(exactly = 0) { arrangement.analyticsManager.sendEvent(any()) }
        coVerify(exactly = 1) { arrangement.observeRecentlyEndedCallMetadata() }
        coVerify(exactly = 0) { arrangement.updateNextTimeCallFeedback(any()) }
        coVerify(exactly = 0) { replacement.observeRecentlyEndedCallMetadata() }
        coVerify(exactly = 0) { replacement.updateNextTimeCallFeedback(any()) }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `cancelling the VM drains a waiting skip callback without reporting feedback`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val (arrangement, viewModel) = Arrangement().arrange()
        coEvery { arrangement.observeRecentlyEndedCallMetadata() } returns flow {
            entered.complete(Unit)
            try { awaitCancellation() } finally { drained.complete(Unit) }
        }
        viewModel.skipCallFeedback(false)
        entered.await()
        viewModel.viewModelScope.cancel()
        runCurrent()
        assertTrue(drained.isCompleted)
        arrangement.admission.publish(TestUser.USER_ID, arrangement.session)
        runCurrent()
        coVerify(exactly = 1) { arrangement.observeRecentlyEndedCallMetadata() }
        coVerify(exactly = 0) { arrangement.analyticsManager.sendEvent(any()) }
        coVerify(exactly = 0) { arrangement.updateNextTimeCallFeedback(any()) }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `genuine captured analytics failure escapes the feedback observer`() {
        val failure = IllegalStateException("analytics failed")
        val observed = assertThrows(IllegalStateException::class.java) {
            runTest {
                val arrangement = Arrangement().withObserveAskCallFeedbackUseCaseReturning(
                    ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.CallDurationIsLessThanOneMinute(2L)
                )
                coEvery { arrangement.isAnalyticsAvailable(any(), any()) } throws failure
                val (_, viewModel) = arrangement.arrange()
                runCurrent()
                viewModel.viewModelScope.cancel()
            }
        }
        assertEquals(failure.message, observed.message)
    }

    private inner class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var analyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var currentSessionFlow: CurrentSessionFlowUseCase

        @MockK
        lateinit var isAnalyticsAvailable: IsAnalyticsAvailableUseCase

        val session = mockk<UserSessionScope>()
        private val calls = mockk<CallsScope>()
        val observeRecentlyEndedCallMetadata = mockk<ObserveRecentlyEndedCallMetadataUseCase>()
        val updateNextTimeCallFeedback = mockk<UpdateNextTimeCallFeedbackUseCase>()
        val admission by lazy { FakeSessionAdmission(coreLogic) { session } }
        var rawSessions: Flow<CurrentSessionResult> = flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))

        val viewModel: CallFeedbackViewModel by lazy {
            CallFeedbackViewModel(
                coreLogic = lazyOf(coreLogic),
                currentSessionFlow = lazyOf(currentSessionFlow),
                isAnalyticsAvailable = lazyOf(isAnalyticsAvailable),
                analyticsManager = lazyOf(analyticsManager),
            )
        }

        fun withObserveAskCallFeedbackUseCaseReturning(result: ShouldAskCallFeedbackUseCaseResult) = apply {
            coEvery { calls.observeAskCallFeedbackUseCase() } returns flowOf(result)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { isAnalyticsAvailable(any(), any()) } returns true
            admission
            every { currentSessionFlow() } answers { rawSessions }
            coEvery { coreLogic.getGlobalScope().observeAllValidSessionsFlow() } answers {
                rawSessions.map { result ->
                    GetAllSessionsResult.Success(
                        listOfNotNull((result as? CurrentSessionResult.Success)?.accountInfo?.takeIf { it.isValid() })
                    )
                }
            }
            coEvery { session.observeSyncState() } returns flowOf(SyncState.Live)
            every { session.calls } returns calls
            every { calls.observeRecentlyEndedCallMetadata } returns observeRecentlyEndedCallMetadata
            every { calls.updateNextTimeCallFeedback } returns updateNextTimeCallFeedback
            coEvery { calls.observeAskCallFeedbackUseCase() } returns flowOf()
            coEvery { updateNextTimeCallFeedback(any()) } returns Unit
            coEvery { observeRecentlyEndedCallMetadata() } returns flowOf(recentlyEndedCallMetadata)
        }

        fun withFeedbackFlow(result: Flow<ShouldAskCallFeedbackUseCaseResult>) = apply {
            coEvery { calls.observeAskCallFeedbackUseCase() } returns result
        }

        fun withMetadata(metadata: RecentlyEndedCallMetadata) = apply {
            coEvery { observeRecentlyEndedCallMetadata() } returns flowOf(metadata)
        }

        fun arrange() = this to viewModel
    }

    companion object {
        const val CALL_DURATION = 100L
        val recentlyEndedCallMetadata = RecentlyEndedCallMetadata(
            callEndReason = 1,
            callDetails = RecentlyEndedCallMetadata.CallDetails(
                isCallScreenShare = false,
                screenShareDurationInSeconds = 20L,
                callScreenShareUniques = 5,
                isOutgoingCall = true,
                callDurationInSeconds = CALL_DURATION,
                callParticipantsCount = 5,
                conversationServices = 1,
                callAVSwitchToggle = false,
                callVideoEnabled = false
            ),
            conversationDetails = RecentlyEndedCallMetadata.ConversationDetails(
                conversationType = Conversation.Type.OneOnOne,
                conversationSize = 5,
                conversationGuests = 2,
                conversationGuestsPro = 1
            ),
            isTeamMember = true
        )
    }
}
