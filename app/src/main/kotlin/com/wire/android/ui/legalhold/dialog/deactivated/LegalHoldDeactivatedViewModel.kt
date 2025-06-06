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
package com.wire.android.ui.legalhold.dialog.deactivated

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.legalhold.LegalHoldState
import com.wire.kalium.logic.feature.legalhold.MarkLegalHoldChangeAsNotifiedForSelfUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldChangeNotifiedForSelfUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalHoldDeactivatedViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>
) : ViewModel() {

    var state: LegalHoldDeactivatedState by mutableStateOf(LegalHoldDeactivatedState.Hidden)
        private set

    private fun <T> currentSessionFlow(noSession: T, session: suspend UserSessionScope.(UserId) -> Flow<T>): Flow<T> =
        coreLogic.get().getGlobalScope().session.currentSessionFlow()
            .flatMapLatest { currentSessionResult ->
                when (currentSessionResult) {
                    is CurrentSessionResult.Failure.Generic -> {
                        appLogger.e("$TAG: Failed to get current session")
                        flowOf(noSession)
                    }

                    CurrentSessionResult.Failure.SessionNotFound -> flowOf(noSession)
                    is CurrentSessionResult.Success ->
                        currentSessionResult.accountInfo.userId.let { coreLogic.get().getSessionScope(it).session(it) }
                }
            }

    init {
        viewModelScope.launch {
            currentSessionFlow(noSession = LegalHoldDeactivatedState.Hidden) { userId ->
                observeLegalHoldChangeNotifiedForSelf()
                    .mapLatest {
                        when (it) {
                            is ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.Failure -> {
                                appLogger.e("$TAG: Failed to get legal hold change notified data: ${it.failure}")
                                LegalHoldDeactivatedState.Hidden
                            }
                            ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.AlreadyNotified -> LegalHoldDeactivatedState.Hidden
                            is ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.ShouldNotify ->
                                when (it.legalHoldState) {
                                    is LegalHoldState.Disabled -> LegalHoldDeactivatedState.Visible(userId)
                                    is LegalHoldState.Enabled -> { // for enabled we don't show the dialog, just mark as already notified
                                        coreLogic.get().getSessionScope(userId).markLegalHoldChangeAsNotifiedForSelf()
                                        LegalHoldDeactivatedState.Hidden
                                    }
                                }
                        }
                    }
            }.collectLatest { state = it }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            (state as? LegalHoldDeactivatedState.Visible)?.let {
                coreLogic.get().getSessionScope(it.userId).markLegalHoldChangeAsNotifiedForSelf().let {
                    if (it is MarkLegalHoldChangeAsNotifiedForSelfUseCase.Result.Success) {
                        state = LegalHoldDeactivatedState.Hidden
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LegalHoldDeactivatedViewModel"
    }
}
