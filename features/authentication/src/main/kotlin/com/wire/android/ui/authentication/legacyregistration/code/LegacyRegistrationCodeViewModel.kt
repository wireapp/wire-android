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
    val input: LegacyRegistrationCodeInput<LinksT>, defaultServerConfig: LinksT,
    private val gateway: LegacyRegistrationCodeGateway<LinksT, FailureT, UserT, CredentialsT>,
) : ViewModel() {
    val serverConfig = input.customServerConfig ?: defaultServerConfig
    val codeTextState = TextFieldState()
    var state: LegacyRegistrationCodeState<UserT, FailureT> by mutableStateOf(LegacyRegistrationCodeState(email = input.email))
        private set
    init { viewModelScope.launch { gateway.onCodeVerificationShown(); codeTextState.textAsFlow().collectLatest { if (it.length ==
    state.codeLength) submit() } } }
    fun resendCode() { state = state.copy(loading = true); viewModelScope.launch { val result =
    gateway.requestActivationCode(serverConfig, input.email); if (result is LegacyCodeActivationResult.AuthScopeUnavailable)
    return@launch; state = state.copy(loading = false, result = result.toResult()) } }
    fun clearError() { state = state.copy(result = LegacyRegistrationCodeState.Result.None) }
    fun clearCodeField() { codeTextState.clearText() }
    private fun submit() { state = state.copy(loading = true); viewModelScope.launch {
        val credentials = when (val result = gateway.register(serverConfig, LegacyPersonalRegistrationRequest(input.name,
    input.password, input.email) { codeTextState.text.toString() })) {
            is LegacyRegistrationResult.Success -> result.credentials
            LegacyRegistrationResult.AuthScopeUnavailable -> return@launch
            else -> { gateway.onCodeVerificationFailed(); return@launch fail(result.toResult()) }
        }
        val userId = when (val result = gateway.storeSession(credentials)) { is LegacyStoreSessionResult.Success -> result.userId;
    else -> return@launch fail(result.toResult()) }
        state = when (val result = gateway.registerClient(userId, input.password)) {
            LegacyRegisterClientResult.Success, LegacyRegisterClientResult.E2EICertificateRequired -> state.copy(result =
    LegacyRegistrationCodeState.Result.Success(userId))
            LegacyRegisterClientResult.TooManyDevices -> state.copy(loading = false, result =
    LegacyRegistrationCodeState.Result.TooManyDevices(userId))
            is LegacyRegisterClientResult.Generic -> state.copy(loading = false, result =
    LegacyRegistrationCodeState.Result.Generic(result.failure))
        }
    } }
    private fun fail(result: LegacyRegistrationCodeState.Result<UserT, FailureT>) { state = state.copy(loading = false, result =
    result) }
    private fun LegacyCodeActivationResult<FailureT>.toResult(): LegacyRegistrationCodeState.Result<UserT, FailureT> = when (this) {
    LegacyCodeActivationResult.Sent -> LegacyRegistrationCodeState.Result.None; LegacyCodeActivationResult.AlreadyInUse ->
    LegacyRegistrationCodeState.Result.AccountAlreadyExists; LegacyCodeActivationResult.Blacklisted ->
    LegacyRegistrationCodeState.Result.Blacklisted; LegacyCodeActivationResult.DomainBlocked ->
    LegacyRegistrationCodeState.Result.DomainBlocked; LegacyCodeActivationResult.InvalidEmail ->
    LegacyRegistrationCodeState.Result.InvalidEmail; is LegacyCodeActivationResult.Generic ->
    LegacyRegistrationCodeState.Result.Generic(failure); LegacyCodeActivationResult.AuthScopeUnavailable -> error("handled") }
    private fun LegacyRegistrationResult<FailureT, CredentialsT>.toResult(): LegacyRegistrationCodeState.Result<UserT, FailureT> =
    when (this) { LegacyRegistrationResult.InvalidActivationCode -> LegacyRegistrationCodeState.Result.InvalidActivationCode;
    LegacyRegistrationResult.AccountAlreadyExists -> LegacyRegistrationCodeState.Result.AccountAlreadyExists;
    LegacyRegistrationResult.Blacklisted -> LegacyRegistrationCodeState.Result.Blacklisted; LegacyRegistrationResult.DomainBlocked
    -> LegacyRegistrationCodeState.Result.DomainBlocked; LegacyRegistrationResult.InvalidEmail ->
    LegacyRegistrationCodeState.Result.InvalidEmail; LegacyRegistrationResult.TeamMembersLimitReached ->
    LegacyRegistrationCodeState.Result.TeamMembersLimit; LegacyRegistrationResult.UserCreationRestricted ->
    LegacyRegistrationCodeState.Result.CreationRestricted; is LegacyRegistrationResult.Generic ->
    LegacyRegistrationCodeState.Result.Generic(failure); is LegacyRegistrationResult.Success,
    LegacyRegistrationResult.AuthScopeUnavailable -> error("handled") }
    private fun LegacyStoreSessionResult<FailureT, UserT>.toResult(): LegacyRegistrationCodeState.Result<UserT, FailureT> = when
    (this) { is LegacyStoreSessionResult.Success -> error("handled"); LegacyStoreSessionResult.UserAlreadyExists ->
    LegacyRegistrationCodeState.Result.UserAlreadyExists; is LegacyStoreSessionResult.Generic ->
    LegacyRegistrationCodeState.Result.Generic(failure) }
}
