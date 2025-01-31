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

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.analytics.IsAnalyticsAvailableUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.ShouldAskCallFeedbackUseCaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallFeedbackViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentSessionFlow: CurrentSessionFlowUseCase,
    private val isAnalyticsAvailable: IsAnalyticsAvailableUseCase,
    private val analyticsManager: AnonymousAnalyticsManager
) : ViewModel() {

    val showCallFeedbackFlow = MutableSharedFlow<Unit>()

    private var currentUserId by mutableStateOf<UserId?>(null)

    init {
        viewModelScope.launch {
            currentSessionFlow()
                .distinctUntilChanged()
                .collectLatest { session ->
                    if (session is CurrentSessionResult.Success && session.accountInfo.isValid()) {
                        currentUserId = session.accountInfo.userId
                        coreLogic.getSessionScope(currentUserId!!).observeSyncState().firstOrNull { it == SyncState.Live }?.let {
                            observeAskCallFeedback(currentUserId!!)
                        }
                    } else {
                        currentUserId = null
                    }
                }
        }
    }

    private suspend fun observeAskCallFeedback(userId: UserId) =
        coreLogic.getSessionScope(userId).calls.observeAskCallFeedbackUseCase().collect { shouldAskFeedback ->
            if (!isAnalyticsAvailable(userId)) {
                return@collect
            }

            when (shouldAskFeedback) {
                is ShouldAskCallFeedbackUseCaseResult.ShouldAskCallFeedback -> {
                    Log.d("callFeedbackViewModel", "observeAskCallFeedback: ShouldAskCallFeedback")
                    showCallFeedbackFlow.emit(Unit)
                }

                is ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.CallDurationIsLessThanOneMinute -> {
                    Log.d(
                        "callFeedbackViewModel",
                        "observeAskCallFeedback: shortCall = duration is ${shouldAskFeedback.callDurationInSeconds.toInt()}"
                    )
                    currentUserId?.let {
                        val recentlyEndedCallMetadata = coreLogic.getSessionScope(it).calls.observeRecentlyEndedCallMetadata().first()
                        analyticsManager.sendEvent(
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
                }

                is ShouldAskCallFeedbackUseCaseResult.ShouldNotAskCallFeedback.NextTimeForCallFeedbackIsNotReached -> {
                    Log.d(
                        "callFeedbackViewModel",
                        "observeAskCallFeedback: NextTimeForCallFeedbackIsNotReached = duration is ${shouldAskFeedback.callDurationInSeconds.toInt()}"
                    )
                    currentUserId?.let {
                        val recentlyEndedCallMetadata = coreLogic.getSessionScope(it).calls.observeRecentlyEndedCallMetadata().first()

                        // call not established
                        if (shouldAskFeedback.callDurationInSeconds.toInt() == 0) {
                            analyticsManager.sendEvent(
                                AnalyticsEvent.CallQualityFeedback.Muted(
                                    callDuration = shouldAskFeedback.callDurationInSeconds.toInt(),
                                    isTeamMember = recentlyEndedCallMetadata.isTeamMember,
                                    participantsCount = 0,
                                    isScreenSharedDuringCall = false,
                                    isCameraEnabledDuringCall = false
                                )
                            )
                        } else {
                            analyticsManager.sendEvent(
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

                    }

                }
            }
        }

    fun rateCall(rate: Int, doNotAsk: Boolean) {
        currentUserId?.let {
            viewModelScope.launch {
                val recentlyEndedCallMetadata = coreLogic.getSessionScope(it).calls.observeRecentlyEndedCallMetadata().first()
                analyticsManager.sendEvent(
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
                coreLogic.getSessionScope(it).calls.updateNextTimeCallFeedback(doNotAsk)
            }
        }
    }

    fun skipCallFeedback(doNotAsk: Boolean) {
        currentUserId?.let {
            viewModelScope.launch {
                val recentlyEndedCallMetadata = coreLogic.getSessionScope(it).calls.observeRecentlyEndedCallMetadata().first()
                coreLogic.getSessionScope(it).calls.updateNextTimeCallFeedback(doNotAsk)
                analyticsManager.sendEvent(
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
