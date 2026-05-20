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
package com.wire.shared.auth.email

import com.wire.shared.auth.SharedViewModel
import com.wire.shared.auth.cancelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@Inject
class LoginEmailViewModelFactory(
    private val gateway: LoginEmailGateway,
) {
    fun create(
        userIdentifier: String = "",
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    ): SharedViewModel<LoginEmailState, LoginEmailEffect, LoginEmailIntent> {
        val scope = CoroutineScope(SupervisorJob() + coroutineContext)
        val state = MutableStateFlow(
            LoginEmailState(
                userIdentifier = userIdentifier,
                userIdentifierEnabled = userIdentifier.isBlank(),
                loginEnabled = false,
            )
        )
        val effects = MutableSharedFlow<LoginEmailEffect>(extraBufferCapacity = 1)

        return SharedViewModel(
            state = state.asStateFlow(),
            effects = effects.asSharedFlow(),
            onIntent = { intent -> handleIntent(intent, state, effects, scope) },
            onClose = { scope.cancelScope() },
        )
    }

    private fun handleIntent(
        intent: LoginEmailIntent,
        state: MutableStateFlow<LoginEmailState>,
        effects: MutableSharedFlow<LoginEmailEffect>,
        scope: CoroutineScope,
    ) {
        when (intent) {
            is LoginEmailIntent.UserIdentifierChanged -> onUserIdentifierChanged(state, intent.value)
            is LoginEmailIntent.PasswordChanged -> onPasswordChanged(state, intent.value)
            is LoginEmailIntent.ProxyIdentifierChanged -> state.update { it.copy(proxyIdentifier = intent.value) }
            is LoginEmailIntent.ProxyPasswordChanged -> state.update { it.copy(proxyPassword = intent.value) }
            is LoginEmailIntent.SecondFactorCodeChanged -> onSecondFactorCodeChanged(state, intent.value)
            is LoginEmailIntent.SubmitLogin -> submitLogin(state, effects, scope, intent.usernameAllowed)
            LoginEmailIntent.ClearLoginErrors -> state.update { it.copy(flowState = LoginEmailFlowState.Default) }
            LoginEmailIntent.CancelLogin -> state.update { it.copy(flowState = LoginEmailFlowState.Canceled) }
            LoginEmailIntent.SecondFactorBackPressed -> onSecondFactorBackPressed(state)
            LoginEmailIntent.ResendSecondFactorCode -> requestSecondFactorCode(state, scope)
        }
    }

    private fun onUserIdentifierChanged(
        state: MutableStateFlow<LoginEmailState>,
        value: String,
    ) {
        state.update {
            it.copy(
                userIdentifier = value,
                loginEnabled = it.canSubmit(userIdentifier = value),
                flowState = LoginEmailFlowState.Default,
            )
        }
    }

    private fun onPasswordChanged(
        state: MutableStateFlow<LoginEmailState>,
        value: String,
    ) {
        state.update {
            it.copy(
                password = value,
                loginEnabled = it.canSubmit(password = value),
                flowState = LoginEmailFlowState.Default,
            )
        }
    }

    private fun onSecondFactorCodeChanged(
        state: MutableStateFlow<LoginEmailState>,
        value: String,
    ) {
        state.update {
            it.copy(
                secondFactorVerificationCode = it.secondFactorVerificationCode.copy(
                    code = value,
                    isCurrentCodeInvalid = false,
                )
            )
        }
    }

    private fun onSecondFactorBackPressed(state: MutableStateFlow<LoginEmailState>) {
        state.update {
            it.copy(
                secondFactorVerificationCode = LoginEmailVerificationCodeState(),
                flowState = LoginEmailFlowState.Default,
            )
        }
    }

    private fun submitLogin(
        state: MutableStateFlow<LoginEmailState>,
        effects: MutableSharedFlow<LoginEmailEffect>,
        scope: CoroutineScope,
        usernameAllowed: Boolean,
    ) {
        val currentState = state.value
        if (!currentState.canSubmit()) {
            state.update { it.copy(flowState = LoginEmailFlowState.Error(LoginEmailError.InvalidCredentials)) }
            return
        }
        state.update { it.copy(flowState = LoginEmailFlowState.Loading, loginEnabled = false) }
        scope.launch {
            handleLoginResult(
                result = gateway.login(
                    userIdentifier = currentState.userIdentifier,
                    password = currentState.password,
                    secondFactorVerificationCode = currentState.secondFactorVerificationCode.code.takeIf { it.isNotBlank() },
                    usernameAllowed = usernameAllowed,
                ),
                state = state,
                effects = effects,
            )
        }
    }

    private fun handleLoginResult(
        result: LoginEmailGatewayResult,
        state: MutableStateFlow<LoginEmailState>,
        effects: MutableSharedFlow<LoginEmailEffect>,
    ) {
        when (result) {
            is LoginEmailGatewayResult.Failure -> setLoginError(state, result.error)
            LoginEmailGatewayResult.RemoveDeviceNeeded -> {
                effects.tryEmit(LoginEmailEffect.RemoveDeviceNeeded)
                setLoginError(state, LoginEmailError.TooManyDevices)
            }
            is LoginEmailGatewayResult.SecondFactorRequired -> setSecondFactorRequired(state, result)
            is LoginEmailGatewayResult.Success -> setLoginSuccess(state, effects, result)
        }
    }

    private fun setLoginError(
        state: MutableStateFlow<LoginEmailState>,
        error: LoginEmailError,
    ) {
        state.update {
            val flowState = LoginEmailFlowState.Error(error)
            it.copy(
                flowState = flowState,
                loginEnabled = it.canSubmit(flowState = flowState),
            )
        }
    }

    private fun setSecondFactorRequired(
        state: MutableStateFlow<LoginEmailState>,
        result: LoginEmailGatewayResult.SecondFactorRequired,
    ) {
        state.update {
            it.copy(
                flowState = LoginEmailFlowState.Default,
                loginEnabled = it.canSubmit(flowState = LoginEmailFlowState.Default),
                secondFactorVerificationCode = it.secondFactorVerificationCode.copy(
                    isCodeInputNecessary = true,
                    emailUsed = result.email,
                    isCurrentCodeInvalid = result.isCurrentCodeInvalid,
                ),
            )
        }
    }

    private fun setLoginSuccess(
        state: MutableStateFlow<LoginEmailState>,
        effects: MutableSharedFlow<LoginEmailEffect>,
        result: LoginEmailGatewayResult.Success,
    ) {
        val flowState = LoginEmailFlowState.Success(
            initialSyncCompleted = result.initialSyncCompleted,
            isE2EIRequired = result.isE2EIRequired,
        )
        state.update { it.copy(flowState = flowState, loginEnabled = false) }
        effects.tryEmit(
            LoginEmailEffect.LoginSucceeded(
                initialSyncCompleted = result.initialSyncCompleted,
                isE2EIRequired = result.isE2EIRequired,
            )
        )
    }

    private fun requestSecondFactorCode(
        state: MutableStateFlow<LoginEmailState>,
        scope: CoroutineScope,
    ) {
        scope.launch {
            when (val result = gateway.requestSecondFactorCode(state.value.userIdentifier)) {
                is LoginEmailGatewayResult.Failure ->
                    state.update { it.copy(flowState = LoginEmailFlowState.Error(result.error)) }

                is LoginEmailGatewayResult.SecondFactorRequired ->
                    state.update {
                        it.copy(
                            flowState = LoginEmailFlowState.Default,
                            secondFactorVerificationCode = it.secondFactorVerificationCode.copy(
                                isCodeInputNecessary = true,
                                emailUsed = result.email,
                                isCurrentCodeInvalid = result.isCurrentCodeInvalid,
                            ),
                        )
                    }

                LoginEmailGatewayResult.RemoveDeviceNeeded,
                is LoginEmailGatewayResult.Success ->
                    Unit
            }
        }
    }
}

private fun LoginEmailState.canSubmit(
    userIdentifier: String = this.userIdentifier,
    password: String = this.password,
    flowState: LoginEmailFlowState = this.flowState,
): Boolean =
    userIdentifier.isNotBlank() && password.isNotBlank() && flowState !is LoginEmailFlowState.Loading
