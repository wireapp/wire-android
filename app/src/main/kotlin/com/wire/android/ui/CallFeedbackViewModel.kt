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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.session.runWithValidSession
import com.wire.android.ui.analytics.IsAnalyticsAvailableUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.ShouldAskCallFeedbackUseCaseResult
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class CallFeedbackViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val isAnalyticsAvailable: Lazy<IsAnalyticsAvailableUseCase>,
    private val analyticsManager: Lazy<AnonymousAnalyticsManager>,
) : ViewModel() {

    val showCallFeedbackFlow = MutableSharedFlow<Unit>()

    private var currentUserId by mutableStateOf<UserId?>(null)

    init {
        viewModelScope.launch {
            observeAskCallFeedback()
        }
    }

    private suspend fun observeAskCallFeedback() {
        currentSessionFlow.value.invoke().distinctUntilChanged().collectLatest { result ->
            val account = (result as? CurrentSessionResult.Success)?.accountInfo
            currentUserId = account?.takeIf { it.isValid() }?.userId
            if (account?.isValid() == true) {
                val userId = account.userId
                coreLogic.value.observeValidSession(userId) {
                    val session = this
                    session.observeSyncState().filter { it == SyncState.Live }
                        .flatMapLatest { session.calls.observeAskCallFeedbackUseCase() }
                        .mapLatest { handleFeedback(userId, session, it) }
                        .flowOn(Dispatchers.Main.immediate)
                }.collect { }
            }
        }
    }

    private suspend fun handleFeedback(
        userId: UserId,
        session: UserSessionScope,
        shouldAskFeedback: ShouldAskCallFeedbackUseCaseResult,
    ) {
        if (!isAnalyticsAvailable.value.invoke(userId, session)) return
        when (shouldAskFeedback) {
            is ShouldAskCallFeedbackUseCaseResult.ShouldAskCallFeedback -> {
                showCallFeedbackFlow.emit(Unit)
            }

            is ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.CallDurationIsLessThanOneMinute -> {
                val recentlyEndedCallMetadata =
                    session.calls.observeRecentlyEndedCallMetadata().first()
                analyticsManager.value.sendEvent(
                    with(recentlyEndedCallMetadata) {
                        AnalyticsEvent.CallQualityFeedback.TooShort(
                            callDuration = shouldAskFeedback.callDurationInSeconds.toInt(),
                            isTeamMember = isTeamMember,
                            participantsCount = callDetails.callParticipantsCount,
                            isScreenSharedDuringCall = callDetails.isCallScreenShare,
                            isCameraEnabledDuringCall = callDetails.callVideoEnabled
                        )
                    }
                )
            }

            is ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.NextTimeForCallFeedbackIsNotReached -> {
                val recentlyEndedCallMetadata =
                    session.calls.observeRecentlyEndedCallMetadata().first()
                analyticsManager.value.sendEvent(
                    with(recentlyEndedCallMetadata) {
                        AnalyticsEvent.CallQualityFeedback.Muted(
                            callDuration = shouldAskFeedback.callDurationInSeconds.toInt(),
                            isTeamMember = isTeamMember,
                            participantsCount = callDetails.callParticipantsCount,
                            isScreenSharedDuringCall = callDetails.isCallScreenShare,
                            isCameraEnabledDuringCall = callDetails.callVideoEnabled
                        )
                    }
                )
            }
        }
    }

    fun rateCall(rate: Int, doNotAsk: Boolean) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                coreLogic.value.runWithValidSession(userId) {
                    val recentlyEndedCallMetadata = calls.observeRecentlyEndedCallMetadata().first()
                    analyticsManager.value.sendEvent(
                        with(recentlyEndedCallMetadata) {
                            AnalyticsEvent.CallQualityFeedback.Answered(
                                score = rate,
                                callDuration = callDetails.callDurationInSeconds.toInt(),
                                isTeamMember = isTeamMember,
                                participantsCount = callDetails.callParticipantsCount,
                                isScreenSharedDuringCall = callDetails.isCallScreenShare,
                                isCameraEnabledDuringCall = callDetails.callVideoEnabled
                            )
                        }
                    )
                    calls.updateNextTimeCallFeedback(doNotAsk)
                }
            }
        }
    }

    fun skipCallFeedback(doNotAsk: Boolean) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                coreLogic.value.runWithValidSession(userId) {
                    val recentlyEndedCallMetadata = calls.observeRecentlyEndedCallMetadata().first()
                    calls.updateNextTimeCallFeedback(doNotAsk)
                    analyticsManager.value.sendEvent(
                        with(recentlyEndedCallMetadata) {
                            AnalyticsEvent.CallQualityFeedback.Dismissed(
                                callDuration = callDetails.callDurationInSeconds.toInt(),
                                isTeamMember = isTeamMember,
                                participantsCount = callDetails.callParticipantsCount,
                                isScreenSharedDuringCall = callDetails.isCallScreenShare,
                                isCameraEnabledDuringCall = callDetails.callVideoEnabled
                            )
                        }
                    )
                }
            }
        }
    }
}
