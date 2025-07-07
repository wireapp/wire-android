/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.registration.details

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.config.orDefault
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.model.AnalyticsEvent.RegistrationPersonalAccount
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class CreateAccountDataDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val validatePassword: ValidatePasswordUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val globalDataStore: GlobalDataStore,
    private val registrationAnalyticsManager: RegistrationAnalyticsManagerUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) : ViewModel() {

    val createAccountNavArgs: CreateAccountDataNavArgs = savedStateHandle.navArgs()

    private var withPasswordTries = false
    val emailTextState: TextFieldState = TextFieldState(createAccountNavArgs.userRegistrationInfo.email)
    val nameTextState: TextFieldState = TextFieldState()
    val passwordTextState: TextFieldState = TextFieldState()
    val confirmPasswordTextState: TextFieldState = TextFieldState()

    var detailsState: CreateAccountDataDetailViewState by mutableStateOf(CreateAccountDataDetailViewState())

    val serverConfig: ServerConfig.Links = createAccountNavArgs.customServerConfig.orDefault()
    fun tosUrl(): String = serverConfig.tos
    fun teamCreationUrl(): String = serverConfig.teams

    init {
        viewModelScope.launch {
            combine(
                emailTextState.textAsFlow(),
                nameTextState.textAsFlow(),
                passwordTextState.textAsFlow(),
                confirmPasswordTextState.textAsFlow(),
            ) { email, name, password, confirmPassword ->
                email.isNotBlank() && name.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
            }.collect { fieldsNotEmpty ->
                detailsState = detailsState.copy(
                    error = CreateAccountDataDetailViewState.DetailsError.None,
                    continueEnabled = fieldsNotEmpty && !detailsState.loading
                )
            }
        }
    }

    private fun onEmailContinue() {
        detailsState = detailsState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            delay(ANALYTICS_INIT_WARMUP_THRESHOLD)
            val email = emailTextState.text.toString().trim().lowercase()
            val emailError = when (validateEmail(email)) {
                true -> CreateAccountDataDetailViewState.DetailsError.None
                false -> CreateAccountDataDetailViewState.DetailsError.EmailFieldError.InvalidEmailError
            }
            detailsState = detailsState.copy(
                loading = false,
                continueEnabled = true,
                termsDialogVisible = !detailsState.termsAccepted && emailError is CreateAccountDataDetailViewState.DetailsError.None,
                error = emailError
            )

            registrationAnalyticsManager.sendEventIfEnabled(RegistrationPersonalAccount.AccountSetup(withPasswordTries))
            when {
                detailsState.termsAccepted -> {
                    onTermsAccept()
                }

                else -> {
                    registrationAnalyticsManager.sendEventIfEnabled(RegistrationPersonalAccount.TermsOfUseDialog)
                }
            }
        }.invokeOnCompletion {
            detailsState = detailsState.copy(loading = false)
        }
    }

    private fun updateTrackingStatusBasedOnPrivacyPolicyAccepted() {
        viewModelScope.launch {
            println("ym. is privacy policy accepted: ${detailsState.privacyPolicyAccepted}")
            globalDataStore.setAnonymousRegistrationEnabled(detailsState.privacyPolicyAccepted)
        }
    }

    fun onTermsAccept() {
        detailsState = detailsState.copy(loading = true, continueEnabled = false, termsDialogVisible = false, termsAccepted = true)
        viewModelScope.launch {
            val authScope = coreLogic.versionedAuthenticationScope(serverConfig)(null).let {
                when (it) {
                    is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                    is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                        return@launch
                    }
                }
            }

            val email = emailTextState.text.toString().trim().lowercase()
            val emailError = authScope.registerScope.requestActivationCode(email).toEmailError()
            detailsState = detailsState.copy(loading = false, continueEnabled = true, error = emailError)
            if (emailError is CreateAccountDataDetailViewState.DetailsError.None) {
                detailsState = detailsState.copy(success = true)
            }
        }
    }

    fun onCodeSentHandled() {
        detailsState = detailsState.copy(success = false)
    }

    fun onDetailsContinue() {
        detailsState = detailsState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            updateTrackingStatusBasedOnPrivacyPolicyAccepted()
            val detailsError = when {
                !validatePassword(passwordTextState.text.toString()).isValid ->
                    CreateAccountDataDetailViewState.DetailsError.PasswordError.InvalidPasswordError

                passwordTextState.text.toString() != confirmPasswordTextState.text.toString() ->
                    CreateAccountDataDetailViewState.DetailsError.PasswordError.PasswordsNotMatchingError

                else -> CreateAccountDataDetailViewState.DetailsError.None
            }
            detailsState = detailsState.copy(
                loading = false,
                continueEnabled = true,
                error = detailsError
            )
            if (detailsState.error is CreateAccountDataDetailViewState.DetailsError.None) {
                onEmailContinue()
            } else {
                withPasswordTries = true
            }
        }
    }

    fun onDetailsErrorDismiss() {
        detailsState = detailsState.copy(error = CreateAccountDataDetailViewState.DetailsError.None)
    }

    fun onTermsDialogDismiss() {
        detailsState = detailsState.copy(termsDialogVisible = false)
    }

    fun onPrivacyPolicyAccepted(isAccepted: Boolean) {
        println("ym. changing privacy policy accepted: $isAccepted")
        detailsState = detailsState.copy(privacyPolicyAccepted = isAccepted)
    }

    private fun RequestActivationCodeResult.toEmailError() = when (this) {
        is RequestActivationCodeResult.Failure.AlreadyInUse ->
            CreateAccountDataDetailViewState.DetailsError.EmailFieldError.AlreadyInUseError

        is RequestActivationCodeResult.Failure.BlacklistedEmail ->
            CreateAccountDataDetailViewState.DetailsError.EmailFieldError.BlacklistedEmailError

        is RequestActivationCodeResult.Failure.DomainBlocked ->
            CreateAccountDataDetailViewState.DetailsError.EmailFieldError.DomainBlockedError

        is RequestActivationCodeResult.Failure.InvalidEmail ->
            CreateAccountDataDetailViewState.DetailsError.EmailFieldError.InvalidEmailError

        is RequestActivationCodeResult.Failure.Generic ->
            CreateAccountDataDetailViewState.DetailsError.DialogError.GenericError(this.failure)

        is RequestActivationCodeResult.Success ->
            CreateAccountDataDetailViewState.DetailsError.None
    }

    private companion object {
        val ANALYTICS_INIT_WARMUP_THRESHOLD = 1.seconds
    }
}
