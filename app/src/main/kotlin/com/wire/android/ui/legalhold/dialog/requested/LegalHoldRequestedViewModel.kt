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
package com.wire.android.ui.legalhold.dialog.requested

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.legalhold.ApproveLegalHoldRequestUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldRequestUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalHoldRequestedViewModel @Inject constructor(
    private val validatePassword: ValidatePasswordUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) : ViewModel() {

    var state: LegalHoldRequestedState by mutableStateOf(LegalHoldRequestedState.Hidden)
        private set

    private val legalHoldRequestDataStateFlow = currentSessionFlow(noSession = LegalHoldRequestData.None) { userId ->
        observeLegalHoldRequest()
            .mapLatest { legalHoldRequestResult ->
                when (legalHoldRequestResult) {
                    is ObserveLegalHoldRequestUseCase.Result.Failure -> {
                        appLogger.e("$TAG: Failed to get legal hold request data: ${legalHoldRequestResult.failure}")
                        LegalHoldRequestData.None
                    }

                    ObserveLegalHoldRequestUseCase.Result.NoLegalHoldRequest -> LegalHoldRequestData.None
                    is ObserveLegalHoldRequestUseCase.Result.LegalHoldRequestAvailable ->
                        users.isPasswordRequired()
                            .let {
                                LegalHoldRequestData.Pending(
                                    legalHoldRequestResult.fingerprint.decodeToString(),
                                    (it as? IsPasswordRequiredUseCase.Result.Success)?.value ?: true,
                                    userId,
                                )
                            }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LegalHoldRequestData.None)

    private fun <T> currentSessionFlow(noSession: T, session: UserSessionScope.(UserId) -> Flow<T>): Flow<T> =
        coreLogic.getGlobalScope().session.currentSessionFlow()
            .flatMapLatest { currentSessionResult ->
                when (currentSessionResult) {
                    is CurrentSessionResult.Failure.Generic -> {
                        appLogger.e("$TAG: Failed to get current session")
                        flowOf(noSession)
                    }

                    CurrentSessionResult.Failure.SessionNotFound -> flowOf(noSession)
                    is CurrentSessionResult.Success ->
                        currentSessionResult.accountInfo.userId.let { coreLogic.getSessionScope(it).session(it) }
                }
            }

    init {
        viewModelScope.launch {
            legalHoldRequestDataStateFlow.collectLatest { legalHoldRequestData ->
                state = when (legalHoldRequestData) {
                    is LegalHoldRequestData.Pending -> {
                        LegalHoldRequestedState.Visible(
                            requiresPassword = legalHoldRequestData.isPasswordRequired,
                            legalHoldDeviceFingerprint = legalHoldRequestData.fingerprint,
                            userId = legalHoldRequestData.userId,
                        )
                    }

                    LegalHoldRequestData.None -> LegalHoldRequestedState.Hidden
                }
            }
        }
    }

    private fun LegalHoldRequestedState.ifVisible(action: (LegalHoldRequestedState.Visible) -> Unit) {
        if (this is LegalHoldRequestedState.Visible) action(this)
    }

    fun passwordChanged(password: TextFieldValue) {
        state.ifVisible {
            state = it.copy(password = password, acceptEnabled = validatePassword(password.text).isValid)
        }
    }

    fun notNowClicked() {
        state = LegalHoldRequestedState.Hidden
    }

    fun show() {
        (legalHoldRequestDataStateFlow.value as? LegalHoldRequestData.Pending)?.let {
            state = LegalHoldRequestedState.Visible(
                requiresPassword = it.isPasswordRequired,
                legalHoldDeviceFingerprint = it.fingerprint,
                userId = it.userId,
            )
        }
    }

    fun acceptClicked() {
        state.ifVisible {
            state = it.copy(acceptEnabled = false, loading = true)
            // the accept button is enabled if the password is valid, this check is for safety only
            validatePassword(it.password.text).let { validatePasswordResult ->
                when (validatePasswordResult.isValid) {
                    false ->
                        state = it.copy(
                            loading = false,
                            error = LegalHoldRequestedError.InvalidCredentialsError
                        )

                    true ->
                        viewModelScope.launch {
                            coreLogic.sessionScope(it.userId) {
                                approveLegalHoldRequest(it.password.text).let { approveLegalHoldResult ->
                                    state = when (approveLegalHoldResult) {
                                        is ApproveLegalHoldRequestUseCase.Result.Success ->
                                            LegalHoldRequestedState.Hidden

                                        ApproveLegalHoldRequestUseCase.Result.Failure.InvalidPassword ->
                                            it.copy(
                                                loading = false,
                                                error = LegalHoldRequestedError.InvalidCredentialsError
                                            )

                                        ApproveLegalHoldRequestUseCase.Result.Failure.PasswordRequired ->
                                            it.copy(
                                                loading = false,
                                                requiresPassword = true,
                                                error = LegalHoldRequestedError.InvalidCredentialsError
                                            )

                                        is ApproveLegalHoldRequestUseCase.Result.Failure.GenericFailure -> {
                                            appLogger.e("$TAG: Failed to approve legal hold: ${approveLegalHoldResult.coreFailure}")
                                            it.copy(
                                                loading = false,
                                                error = LegalHoldRequestedError.GenericError(approveLegalHoldResult.coreFailure)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

    private sealed class LegalHoldRequestData {
        data object None : LegalHoldRequestData()
        data class Pending(val fingerprint: String, val isPasswordRequired: Boolean, val userId: UserId) : LegalHoldRequestData()
    }

    companion object {
        private const val TAG = "LegalHoldRequestedViewModel"
    }
}
