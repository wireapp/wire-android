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
package com.wire.shared.auth.sso

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
class LoginSsoViewModelFactory(
    private val backend: LoginSsoBackend,
) {
    fun create(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    ): SharedViewModel<LoginSsoState, LoginSsoEffect, LoginSsoIntent> {
        val scope = CoroutineScope(SupervisorJob() + coroutineContext)
        val state = MutableStateFlow(LoginSsoState())
        val effects = MutableSharedFlow<LoginSsoEffect>(extraBufferCapacity = 1)

        return SharedViewModel(
            state = state.asStateFlow(),
            effects = effects.asSharedFlow(),
            onIntent = { intent ->
                when (intent) {
                    is LoginSsoIntent.SsoCodeChanged -> {
                        val ssoCode = intent.ssoCode
                        state.update {
                            it.copy(
                                ssoCode = ssoCode,
                                loginEnabled = ssoCode.isValidSsoCode(),
                                flowState = LoginSsoFlowState.Default,
                            )
                        }
                    }

                    LoginSsoIntent.SubmitLogin ->
                        submitLogin(state, effects, scope)

                    LoginSsoIntent.ClearLoginErrors ->
                        state.update { it.copy(flowState = LoginSsoFlowState.Default) }

                    LoginSsoIntent.DismissCustomServerDialog ->
                        state.update { it.copy(customServerDialogState = null) }

                    LoginSsoIntent.ConfirmCustomServerDialog ->
                        state.update { it.copy(customServerDialogState = null) }

                    is LoginSsoIntent.AutoFillSsoCode -> {
                        state.update {
                            it.copy(
                                ssoCode = intent.ssoCode,
                                loginEnabled = intent.ssoCode.isValidSsoCode(),
                                flowState = LoginSsoFlowState.Default,
                            )
                        }
                        if (intent.autoInitiateLogin) {
                            submitLogin(state, effects, scope)
                        }
                    }

                    is LoginSsoIntent.CompleteSsoLogin ->
                        completeLogin(intent, state, effects, scope)

                    is LoginSsoIntent.ReportSsoLoginFailure ->
                        state.update {
                            it.copy(flowState = LoginSsoFlowState.Error(LoginSsoError.SsoResultError(intent.code)))
                        }
                }
            },
            onClose = { scope.cancelScope() },
        )
    }

    private fun submitLogin(
        state: MutableStateFlow<LoginSsoState>,
        effects: MutableSharedFlow<LoginSsoEffect>,
        scope: CoroutineScope,
    ) {
        val ssoCode = state.value.ssoCode.trim()
        if (!ssoCode.isValidSsoCode()) {
            state.update { it.copy(flowState = LoginSsoFlowState.Error(LoginSsoError.InvalidSsoCode)) }
            return
        }

        state.update { it.copy(flowState = LoginSsoFlowState.Loading) }
        scope.launch {
            when (val result = backend.initiateLogin(ssoCode)) {
                is LoginSsoBackendResult.Error ->
                    state.update { it.copy(flowState = LoginSsoFlowState.Error(result.reason)) }

                is LoginSsoBackendResult.OpenUrl -> {
                    effects.tryEmit(
                        LoginSsoEffect.OpenUrl(
                            url = result.url,
                            serverLinks = result.serverLinks,
                        )
                    )
                    state.update { it.copy(flowState = LoginSsoFlowState.Default) }
                }

                is LoginSsoBackendResult.Success ->
                    state.update { it.withSuccess(result) }
            }
        }
    }

    private fun completeLogin(
        intent: LoginSsoIntent.CompleteSsoLogin,
        state: MutableStateFlow<LoginSsoState>,
        effects: MutableSharedFlow<LoginSsoEffect>,
        scope: CoroutineScope,
    ) {
        state.update { it.copy(flowState = LoginSsoFlowState.Loading) }
        scope.launch {
            when (val result = backend.completeLogin(intent.cookie, intent.serverConfigId)) {
                is LoginSsoBackendResult.Error ->
                    state.update { it.copy(flowState = LoginSsoFlowState.Error(result.reason)) }

                is LoginSsoBackendResult.OpenUrl -> {
                    effects.tryEmit(
                        LoginSsoEffect.OpenUrl(
                            url = result.url,
                            serverLinks = result.serverLinks,
                        )
                    )
                    state.update { it.copy(flowState = LoginSsoFlowState.Default) }
                }

                is LoginSsoBackendResult.Success ->
                    state.update { it.withSuccess(result) }
            }
        }
    }
}

private fun LoginSsoState.withSuccess(result: LoginSsoBackendResult.Success): LoginSsoState =
    copy(
        flowState = LoginSsoFlowState.Success(
            initialSyncCompleted = result.initialSyncCompleted,
            e2eiRequired = result.e2eiRequired,
        )
    )

private fun String.isValidSsoCode(): Boolean =
    startsWith(SSO_CODE_PREFIX) && removePrefix(SSO_CODE_PREFIX).matches(uuidRegex)

private const val SSO_CODE_PREFIX = "wire-"
private val uuidRegex = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
