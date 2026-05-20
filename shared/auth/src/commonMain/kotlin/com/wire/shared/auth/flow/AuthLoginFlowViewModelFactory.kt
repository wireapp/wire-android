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
package com.wire.shared.auth.flow

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
class AuthLoginFlowViewModelFactory(
    private val backend: AuthLoginFlowBackend,
) {
    fun create(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    ): SharedViewModel<AuthLoginFlowState, AuthLoginFlowEffect, AuthLoginFlowIntent> {
        val scope = CoroutineScope(SupervisorJob() + coroutineContext)
        val state = MutableStateFlow(AuthLoginFlowState())
        val effects = MutableSharedFlow<AuthLoginFlowEffect>(extraBufferCapacity = 1)

        return SharedViewModel(
            state = state.asStateFlow(),
            effects = effects.asSharedFlow(),
            onIntent = { intent ->
                when (intent) {
                    is AuthLoginFlowIntent.IdentifierChanged ->
                        state.update { it.copy(identifier = intent.value, error = null) }

                    is AuthLoginFlowIntent.SsoCodeChanged ->
                        state.update { it.copy(ssoCode = intent.value, error = null) }

                    AuthLoginFlowIntent.SubmitIdentifier ->
                        submitIdentifier(state, effects, scope)

                    AuthLoginFlowIntent.SubmitSsoCode ->
                        submitSsoCode(state, effects, scope)

                    is AuthLoginFlowIntent.PasswordChanged ->
                        state.update { it.copy(password = intent.value, error = null) }

                    is AuthLoginFlowIntent.SubmitCredentials ->
                        submitCredentials(state, effects, scope, intent.usernameAllowed)

                    is AuthLoginFlowIntent.SecondFactorCodeChanged ->
                        state.update { it.copy(secondFactorCode = intent.value, error = null) }

                    is AuthLoginFlowIntent.SubmitSecondFactor ->
                        submitSecondFactor(state, effects, scope, intent.usernameAllowed)

                    AuthLoginFlowIntent.Back ->
                        state.update { it.back() }

                    AuthLoginFlowIntent.Cancel ->
                        state.update { AuthLoginFlowState() }

                    AuthLoginFlowIntent.ClearError ->
                        state.update { it.copy(error = null) }
                }
            },
            onClose = { scope.cancelScope() },
        )
    }

    private fun submitIdentifier(
        state: MutableStateFlow<AuthLoginFlowState>,
        effects: MutableSharedFlow<AuthLoginFlowEffect>,
        scope: CoroutineScope,
    ) {
        val identifier = state.value.identifier.trim()
        if (identifier.isBlank()) {
            state.update { it.copy(error = AuthLoginFlowError.InvalidIdentifier) }
            return
        }
        state.update { it.copy(identifier = identifier, isLoading = true, error = null) }
        scope.launch {
            handleIdentifierResult(
                result = backend.resolveIdentifier(identifier),
                state = state,
                effects = effects,
            )
        }
    }

    private fun submitSsoCode(
        state: MutableStateFlow<AuthLoginFlowState>,
        effects: MutableSharedFlow<AuthLoginFlowEffect>,
        scope: CoroutineScope,
    ) {
        val ssoCode = state.value.ssoCode.trim()
        if (ssoCode.isBlank()) {
            state.update { it.copy(error = AuthLoginFlowError.InvalidIdentifier) }
            return
        }
        state.update { it.copy(ssoCode = ssoCode, isLoading = true, error = null) }
        scope.launch {
            handleIdentifierResult(
                result = backend.initiateSso(ssoCode),
                state = state,
                effects = effects,
            )
        }
    }

    private fun submitCredentials(
        state: MutableStateFlow<AuthLoginFlowState>,
        effects: MutableSharedFlow<AuthLoginFlowEffect>,
        scope: CoroutineScope,
        usernameAllowed: Boolean,
    ) {
        val currentState = state.value
        if (!currentState.canSubmitCredentials) {
            state.update { it.copy(error = AuthLoginFlowError.InvalidCredentials) }
            return
        }
        state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            handleLoginResult(
                result = backend.loginWithEmail(
                    identifier = currentState.identifier,
                    password = currentState.password,
                    secondFactorCode = null,
                    usernameAllowed = usernameAllowed,
                ),
                state = state,
                effects = effects,
            )
        }
    }

    private fun submitSecondFactor(
        state: MutableStateFlow<AuthLoginFlowState>,
        effects: MutableSharedFlow<AuthLoginFlowEffect>,
        scope: CoroutineScope,
        usernameAllowed: Boolean,
    ) {
        val currentState = state.value
        if (!currentState.canSubmitSecondFactor) {
            state.update { it.copy(error = AuthLoginFlowError.InvalidSecondFactorCode) }
            return
        }
        state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            handleLoginResult(
                result = backend.loginWithEmail(
                    identifier = currentState.identifier,
                    password = currentState.password,
                    secondFactorCode = currentState.secondFactorCode,
                    usernameAllowed = usernameAllowed,
                ),
                state = state,
                effects = effects,
            )
        }
    }

    private fun handleIdentifierResult(
        result: AuthLoginFlowIdentifierResult,
        state: MutableStateFlow<AuthLoginFlowState>,
        effects: MutableSharedFlow<AuthLoginFlowEffect>,
    ) {
        when (result) {
            is AuthLoginFlowIdentifierResult.EmailCredentialsRequired ->
                state.update {
                    it.copy(
                        step = AuthLoginFlowStep.EmailCredentialsEntry,
                        identifier = result.identifier,
                        isLoading = false,
                    )
                }

            is AuthLoginFlowIdentifierResult.Failure ->
                state.update { it.copy(isLoading = false, error = result.error) }

            is AuthLoginFlowIdentifierResult.OpenSso -> {
                effects.tryEmit(AuthLoginFlowEffect.OpenSsoUrl(result.url, result.userIdentifier))
                state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleLoginResult(
        result: AuthLoginFlowLoginResult,
        state: MutableStateFlow<AuthLoginFlowState>,
        effects: MutableSharedFlow<AuthLoginFlowEffect>,
    ) {
        when (result) {
            is AuthLoginFlowLoginResult.Failure ->
                state.update { it.copy(isLoading = false, error = result.error) }

            AuthLoginFlowLoginResult.RemoveDeviceNeeded ->
                state.update { it.copy(isLoading = false, error = AuthLoginFlowError.TooManyDevices) }

            is AuthLoginFlowLoginResult.SecondFactorRequired ->
                state.update {
                    it.copy(
                        step = AuthLoginFlowStep.SecondFactorEntry,
                        secondFactorEmail = result.email,
                        secondFactorCode = if (result.isCurrentCodeInvalid) it.secondFactorCode else "",
                        isLoading = false,
                        error = if (result.isCurrentCodeInvalid) AuthLoginFlowError.InvalidSecondFactorCode else null,
                    )
                }

            is AuthLoginFlowLoginResult.Success -> {
                effects.tryEmit(AuthLoginFlowEffect.LoginSucceeded(result.payload))
                state.update {
                    it.copy(
                        step = AuthLoginFlowStep.Success(
                            initialSyncCompleted = result.initialSyncCompleted,
                            isE2EIRequired = result.isE2EIRequired,
                        ),
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }
}

private fun AuthLoginFlowState.back(): AuthLoginFlowState =
    when (step) {
        AuthLoginFlowStep.IdentifierEntry ->
            this

        AuthLoginFlowStep.EmailCredentialsEntry ->
            copy(
                step = AuthLoginFlowStep.IdentifierEntry,
                password = "",
                isLoading = false,
                error = null,
            )

        AuthLoginFlowStep.SecondFactorEntry ->
            copy(
                step = AuthLoginFlowStep.EmailCredentialsEntry,
                secondFactorCode = "",
                secondFactorEmail = "",
                isLoading = false,
                error = null,
            )

        is AuthLoginFlowStep.Success ->
            this
    }
