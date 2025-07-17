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
import com.wire.android.ui.analytics.IsAnalyticsAvailableUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.RecentlyEndedCallMetadata
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.ShouldAskCallFeedbackUseCaseResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class CallFeedbackViewModelTest {

    @Test
    fun `given analytics is not available when use case is observed then it should skip sending feedback event`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        coEvery { arrangement.isAnalyticsAvailable(any()) } returns false

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

    private inner class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var analyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var currentSessionFlow: CurrentSessionFlowUseCase

        @MockK
        lateinit var isAnalyticsAvailable: IsAnalyticsAvailableUseCase

        val viewModel: CallFeedbackViewModel by lazy {
            CallFeedbackViewModel(
                coreLogic = { coreLogic },
                currentSessionFlow = { currentSessionFlow },
                isAnalyticsAvailable = { isAnalyticsAvailable },
                analyticsManager = { analyticsManager },
            )
        }

        fun withObserveAskCallFeedbackUseCaseReturning(result: ShouldAskCallFeedbackUseCaseResult) = apply {
            coEvery { coreLogic.getSessionScope(any()).calls.observeAskCallFeedbackUseCase() } returns flowOf(result)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { isAnalyticsAvailable(any()) } returns true
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
            coEvery { coreLogic.getSessionScope(any()).observeSyncState() } returns flowOf(SyncState.Live)
            coEvery { coreLogic.getSessionScope(any()).calls.observeAskCallFeedbackUseCase() } returns flowOf()
            coEvery { coreLogic.getSessionScope(any()).calls.updateNextTimeCallFeedback(any()) } returns Unit
            coEvery {
                coreLogic.getSessionScope(any()).calls.observeRecentlyEndedCallMetadata()
            } returns flowOf(recentlyEndedCallMetadata)
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
