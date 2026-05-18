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
            onIntent = { intent ->
                when (intent) {
                    is LoginEmailIntent.UserIdentifierChanged ->
                        state.update {
                            it.copy(
                                userIdentifier = intent.value,
                                loginEnabled = it.canSubmit(userIdentifier = intent.value),
                                flowState = LoginEmailFlowState.Default,
                            )
                        }

                    is LoginEmailIntent.PasswordChanged ->
                        state.update {
                            it.copy(
                                password = intent.value,
                                loginEnabled = it.canSubmit(password = intent.value),
                                flowState = LoginEmailFlowState.Default,
                            )
                        }

                    is LoginEmailIntent.ProxyIdentifierChanged ->
                        state.update { it.copy(proxyIdentifier = intent.value) }

                    is LoginEmailIntent.ProxyPasswordChanged ->
                        state.update { it.copy(proxyPassword = intent.value) }

                    is LoginEmailIntent.SecondFactorCodeChanged ->
                        state.update {
                            it.copy(
                                secondFactorVerificationCode = it.secondFactorVerificationCode.copy(
                                    code = intent.value,
                                    isCurrentCodeInvalid = false,
                                )
                            )
                        }

                    is LoginEmailIntent.SubmitLogin ->
                        submitLogin(state, effects, scope, intent.usernameAllowed)

                    LoginEmailIntent.ClearLoginErrors ->
                        state.update { it.copy(flowState = LoginEmailFlowState.Default) }

                    LoginEmailIntent.CancelLogin ->
                        state.update { it.copy(flowState = LoginEmailFlowState.Canceled) }

                    LoginEmailIntent.SecondFactorBackPressed ->
                        state.update {
                            it.copy(
                                secondFactorVerificationCode = LoginEmailVerificationCodeState(),
                                flowState = LoginEmailFlowState.Default,
                            )
                        }

                    LoginEmailIntent.ResendSecondFactorCode ->
                        requestSecondFactorCode(state, scope)
                }
            },
            onClose = { scope.cancelScope() },
        )
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
            when (
                val result = gateway.login(
                    userIdentifier = currentState.userIdentifier,
                    password = currentState.password,
                    secondFactorVerificationCode = currentState.secondFactorVerificationCode.code.takeIf { it.isNotBlank() },
                    usernameAllowed = usernameAllowed,
                )
            ) {
                is LoginEmailGatewayResult.Failure -> {
                    state.update {
                        val flowState = LoginEmailFlowState.Error(result.error)
                        it.copy(
                            flowState = flowState,
                            loginEnabled = it.canSubmit(flowState = flowState),
                        )
                    }
                }

                LoginEmailGatewayResult.RemoveDeviceNeeded -> {
                    effects.tryEmit(LoginEmailEffect.RemoveDeviceNeeded)
                    state.update {
                        val flowState = LoginEmailFlowState.Error(LoginEmailError.TooManyDevices)
                        it.copy(
                            flowState = flowState,
                            loginEnabled = it.canSubmit(flowState = flowState),
                        )
                    }
                }

                is LoginEmailGatewayResult.SecondFactorRequired -> {
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

                is LoginEmailGatewayResult.Success -> {
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
            }
        }
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
