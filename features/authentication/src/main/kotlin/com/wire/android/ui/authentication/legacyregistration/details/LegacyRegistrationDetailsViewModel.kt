/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Legacy personal-registration details policy; deliberately separate from the new account flow. */
open class LegacyRegistrationDetailsViewModel<LinksT, FailureT>(
    val input: LegacyRegistrationDetailsInput<LinksT>,
    defaultServerConfig: LinksT,
    private val gateway: LegacyRegistrationDetailsGateway<LinksT, FailureT>,
    private val analyticsWarmupMillis: Long = ANALYTICS_WARMUP_MILLIS,
) : ViewModel() {
    val serverConfig: LinksT = input.customServerConfig ?: defaultServerConfig
    val emailTextState = TextFieldState(input.email)
    val nameTextState = TextFieldState()
    val passwordTextState = TextFieldState()
    val confirmPasswordTextState = TextFieldState()
    var state: LegacyRegistrationDetailsState<FailureT> by mutableStateOf(LegacyRegistrationDetailsState())
        private set
    private var withPasswordTries = false

    init {
        viewModelScope.launch {
            combine(emailTextState.textAsFlow(), nameTextState.textAsFlow(), passwordTextState.textAsFlow(),
    confirmPasswordTextState.textAsFlow()) { email, name, password, confirmation ->
                email.isNotBlank() && name.isNotBlank() && password.isNotBlank() && confirmation.isNotBlank()
            }.collect { fieldsNotEmpty ->
                state = state.copy(error = LegacyRegistrationDetailsState.DetailsError.None, continueEnabled = fieldsNotEmpty &&
    !state.loading)
            }
        }
    }

    fun onDetailsContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            gateway.setAnonymousRegistrationEnabled(state.privacyPolicyAccepted)
            val error = when {
                !gateway.isPasswordValid(passwordTextState.text.toString()) ->
    LegacyRegistrationDetailsState.DetailsError.PasswordError.InvalidPasswordError
                passwordTextState.text.toString() != confirmPasswordTextState.text.toString() ->
    LegacyRegistrationDetailsState.DetailsError.PasswordError.PasswordsNotMatchingError
                else -> LegacyRegistrationDetailsState.DetailsError.None
            }
            state = state.copy(loading = false, continueEnabled = true, error = error)
            if (error is LegacyRegistrationDetailsState.DetailsError.None) onEmailContinue() else withPasswordTries = true
        }
    }

    private fun onEmailContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            delay(analyticsWarmupMillis)
            val error = if (gateway.isEmailValid(emailTextState.text.toString().trim().lowercase())) {
                LegacyRegistrationDetailsState.DetailsError.None
            } else {
                LegacyRegistrationDetailsState.DetailsError.EmailFieldError.InvalidEmailError
            }
            state = state.copy(
                loading = false,
                continueEnabled = true,
                termsDialogVisible = !state.termsAccepted && error is LegacyRegistrationDetailsState.DetailsError.None,
                error = error,
            )
            gateway.onAccountSetup(withPasswordTries)
            if (state.termsAccepted) onTermsAccept() else gateway.onTermsOfUseDialog()
        }.invokeOnCompletion { state = state.copy(loading = false) }
    }

    fun onTermsAccept() {
        state = state.copy(loading = true, continueEnabled = false, termsDialogVisible = false, termsAccepted = true)
        viewModelScope.launch {
            val error = gateway.requestActivationCode(serverConfig, emailTextState.text.toString().trim().lowercase()).toError()
            if (error == null) {
                state = state.copy(loading = false, continueEnabled = true, error =
    LegacyRegistrationDetailsState.DetailsError.None, success = true)
            } else if (error is MappedError.Value) {
                state = state.copy(loading = false, continueEnabled = true, error = error.error)
            }
        }
    }

    fun onCodeSentHandled() { state = state.copy(success = false) }
    fun onErrorDismiss() { state = state.copy(error = LegacyRegistrationDetailsState.DetailsError.None) }
    fun onTermsDialogDismiss() { state = state.copy(termsDialogVisible = false) }
    fun onPrivacyPolicyAccepted(accepted: Boolean) { state = state.copy(privacyPolicyAccepted = accepted) }

    private sealed interface MappedError<out FailureT> { data object AuthScopeUnavailable : MappedError<Nothing>; data class
    Value<FailureT>(val error: LegacyRegistrationDetailsState.DetailsError) : MappedError<FailureT> }
    private fun LegacyActivationCodeResult<FailureT>.toError(): MappedError<FailureT>? = when (this) {
        LegacyActivationCodeResult.Sent -> null
        LegacyActivationCodeResult.AuthScopeUnavailable -> MappedError.AuthScopeUnavailable
        LegacyActivationCodeResult.AlreadyInUse ->
    MappedError.Value(LegacyRegistrationDetailsState.DetailsError.EmailFieldError.AlreadyInUseError)
        LegacyActivationCodeResult.Blacklisted ->
    MappedError.Value(LegacyRegistrationDetailsState.DetailsError.EmailFieldError.BlacklistedEmailError)
        LegacyActivationCodeResult.DomainBlocked ->
    MappedError.Value(LegacyRegistrationDetailsState.DetailsError.EmailFieldError.DomainBlockedError)
        LegacyActivationCodeResult.InvalidEmail ->
    MappedError.Value(LegacyRegistrationDetailsState.DetailsError.EmailFieldError.InvalidEmailError)
        is LegacyActivationCodeResult.Generic ->
    MappedError.Value(LegacyRegistrationDetailsState.DetailsError.GenericError(failure))
    }

    private companion object { const val ANALYTICS_WARMUP_MILLIS = 1_000L }
}
