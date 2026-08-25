/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.code

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class LegacyRegistrationCodeViewModel<LinksT, FailureT, UserT, CredentialsT>(
    val input: LegacyRegistrationCodeInput<LinksT>,
    defaultServerConfig: LinksT,
    private val gateway: LegacyRegistrationCodeGateway<LinksT, FailureT, UserT, CredentialsT>,
) : ViewModel() {
    val serverConfig = input.customServerConfig ?: defaultServerConfig
    val codeTextState = TextFieldState()

    var state: LegacyRegistrationCodeState<UserT, FailureT> by mutableStateOf(
        LegacyRegistrationCodeState(email = input.email),
    )
        private set

    init {
        viewModelScope.launch {
            gateway.onCodeVerificationShown()
            codeTextState.textAsFlow().collectLatest { code ->
                if (code.length == state.codeLength) submit()
            }
        }
    }

    fun resendCode() {
        state = state.copy(loading = true)
        viewModelScope.launch {
            val result = gateway.requestActivationCode(serverConfig, input.email)
            if (result is LegacyCodeActivationResult.AuthScopeUnavailable) return@launch
            state = state.copy(loading = false, result = result.toResult())
        }
    }

    fun clearError() {
        state = state.copy(result = LegacyRegistrationCodeState.Result.None)
    }

    fun clearCodeField() {
        codeTextState.clearText()
    }

    private fun submit() {
        state = state.copy(loading = true)
        viewModelScope.launch {
            val credentials = register() ?: return@launch
            val userId = storeSession(credentials) ?: return@launch
            registerClient(userId)
        }
    }

    private suspend fun register(): CredentialsT? = when (
        val result = gateway.register(
            serverConfig,
            LegacyPersonalRegistrationRequest(
                name = input.name,
                password = input.password,
                email = input.email,
                activationCode = { codeTextState.text.toString() },
            ),
        )
    ) {
        is LegacyRegistrationResult.Success -> result.credentials
        LegacyRegistrationResult.AuthScopeUnavailable -> null
        else -> {
            gateway.onCodeVerificationFailed()
            fail(result.toResult())
            null
        }
    }

    private suspend fun storeSession(credentials: CredentialsT): UserT? = when (val result = gateway.storeSession(credentials)) {
        is LegacyStoreSessionResult.Success -> result.userId
        else -> {
            fail(result.toResult())
            null
        }
    }

    private suspend fun registerClient(userId: UserT) {
        state = when (val result = gateway.registerClient(userId, input.password)) {
            LegacyRegisterClientResult.Success,
            LegacyRegisterClientResult.E2EICertificateRequired -> state.copy(
                result = LegacyRegistrationCodeState.Result.Success(userId),
            )

            LegacyRegisterClientResult.TooManyDevices -> state.copy(
                loading = false,
                result = LegacyRegistrationCodeState.Result.TooManyDevices(userId),
            )

            is LegacyRegisterClientResult.Generic -> state.copy(
                loading = false,
                result = LegacyRegistrationCodeState.Result.Generic(result.failure),
            )
        }
    }

    private fun fail(result: LegacyRegistrationCodeState.Result<UserT, FailureT>) {
        state = state.copy(loading = false, result = result)
    }
}
