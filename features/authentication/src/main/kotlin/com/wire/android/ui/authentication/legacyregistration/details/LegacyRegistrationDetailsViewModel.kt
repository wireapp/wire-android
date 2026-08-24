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

/** Legacy personal-registration policy; deliberately separate from the newer account flow. */
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
            combine(
                emailTextState.textAsFlow(),
                nameTextState.textAsFlow(),
                passwordTextState.textAsFlow(),
                confirmPasswordTextState.textAsFlow(),
            ) { email, name, password, confirmation ->
                email.isNotBlank() && name.isNotBlank() && password.isNotBlank() && confirmation.isNotBlank()
            }.collect { fieldsNotEmpty ->
                state = state.copy(
                    error = LegacyRegistrationDetailsState.DetailsError.None,
                    continueEnabled = fieldsNotEmpty && !state.loading,
                )
            }
        }
    }
    fun onDetailsContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            gateway.setAnonymousRegistrationEnabled(state.privacyPolicyAccepted)
            val error = validatePasswordFields()
            state = state.copy(loading = false, continueEnabled = true, error = error)
            if (error is LegacyRegistrationDetailsState.DetailsError.None) {
                onEmailContinue()
            } else {
                withPasswordTries = true
            }
        }
    }
    private fun validatePasswordFields(): LegacyRegistrationDetailsState.DetailsError = when {
        !gateway.isPasswordValid(passwordTextState.text.toString()) ->
            LegacyRegistrationDetailsState.DetailsError.PasswordError.InvalidPasswordError
        passwordTextState.text.toString() != confirmPasswordTextState.text.toString() ->
            LegacyRegistrationDetailsState.DetailsError.PasswordError.PasswordsNotMatchingError
        else -> LegacyRegistrationDetailsState.DetailsError.None
    }
    private fun onEmailContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            delay(analyticsWarmupMillis)
            val error = validateEmail()
            state = state.copy(
                loading = false,
                continueEnabled = true,
                termsDialogVisible = !state.termsAccepted && error is LegacyRegistrationDetailsState.DetailsError.None,
                error = error,
            )
            gateway.onAccountSetup(withPasswordTries)
            if (state.termsAccepted) onTermsAccept() else gateway.onTermsOfUseDialog()
        }.invokeOnCompletion {
            state = state.copy(loading = false)
        }
    }
    private fun validateEmail(): LegacyRegistrationDetailsState.DetailsError = if (
        gateway.isEmailValid(emailTextState.text.toString().trim().lowercase())
    ) {
        LegacyRegistrationDetailsState.DetailsError.None
    } else {
        LegacyRegistrationDetailsState.DetailsError.EmailFieldError.InvalidEmailError
    }
    fun onTermsAccept() {
        state = state.copy(
            loading = true,
            continueEnabled = false,
            termsDialogVisible = false,
            termsAccepted = true,
        )
        viewModelScope.launch {
            when (val result = gateway.requestActivationCode(serverConfig, normalizedEmail()).toError()) {
                null -> state = state.copy(
                    loading = false,
                    continueEnabled = true,
                    error = LegacyRegistrationDetailsState.DetailsError.None,
                    success = true,
                )

                is MappedError.Value -> state = state.copy(
                    loading = false,
                    continueEnabled = true,
                    error = result.error,
                )

                MappedError.AuthScopeUnavailable -> Unit
            }
        }
    }
    fun onCodeSentHandled() {
        state = state.copy(success = false)
    }

    fun onErrorDismiss() {
        state = state.copy(error = LegacyRegistrationDetailsState.DetailsError.None)
    }

    fun onTermsDialogDismiss() {
        state = state.copy(termsDialogVisible = false)
    }

    fun onPrivacyPolicyAccepted(accepted: Boolean) {
        state = state.copy(privacyPolicyAccepted = accepted)
    }

    private fun normalizedEmail() = emailTextState.text.toString().trim().lowercase()

    private companion object {
        const val ANALYTICS_WARMUP_MILLIS = 1_000L
    }
}
