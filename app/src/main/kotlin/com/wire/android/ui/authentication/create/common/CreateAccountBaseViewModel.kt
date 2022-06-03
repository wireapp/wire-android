package com.wire.android.ui.authentication.create.common

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewState
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewState
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewState
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase.RegisterClientParam
import com.wire.kalium.logic.feature.register.RegisterAccountUseCase
import com.wire.kalium.logic.feature.register.RegisterParam
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
import com.wire.kalium.logic.feature.session.RegisterTokenResult
import com.wire.kalium.logic.feature.session.RegisterTokenUseCase
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "LongParameterList")
@OptIn(ExperimentalMaterialApi::class)
abstract class CreateAccountBaseViewModel(
    final override val type: CreateAccountFlowType,
    private val navigationManager: NavigationManager,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val requestActivationCodeUseCase: RequestActivationCodeUseCase,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val registerAccountUseCase: RegisterAccountUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val pushTokenUseCase: RegisterTokenUseCase
) : ViewModel(),
    CreateAccountOverviewViewModel,
    CreateAccountEmailViewModel,
    CreateAccountDetailsViewModel,
    CreateAccountCodeViewModel {
    override var emailState: CreateAccountEmailViewState by mutableStateOf(CreateAccountEmailViewState(type))
    override var detailsState: CreateAccountDetailsViewState by mutableStateOf(CreateAccountDetailsViewState(type))
    override var codeState: CreateAccountCodeViewState by mutableStateOf(CreateAccountCodeViewState(type))

    fun closeForm() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }

    // Overview
    final override fun onOverviewContinue() {
        emailState = CreateAccountEmailViewState(type)
        detailsState = CreateAccountDetailsViewState(type)
        codeState = CreateAccountCodeViewState(type)
        onOverviewSuccess()
    }

    abstract fun onOverviewSuccess()

    // Email
    final override fun onEmailChange(newText: TextFieldValue) {
        emailState = emailState.copy(
            email = newText,
            error = CreateAccountEmailViewState.EmailError.None,
            continueEnabled = newText.text.isNotEmpty() && !emailState.loading
        )
        codeState = codeState.copy(email = newText.text)
    }

    final override fun onEmailErrorDismiss() {
        emailState = emailState.copy(error = CreateAccountEmailViewState.EmailError.None)
    }

    final override fun onEmailContinue(serverConfig: ServerConfig) {
        emailState = emailState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val emailError =
                if (validateEmailUseCase(emailState.email.text.trim().lowercase())) CreateAccountEmailViewState.EmailError.None
                else CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError
            emailState = emailState.copy(
                loading = false,
                continueEnabled = true,
                termsDialogVisible = !emailState.termsAccepted && emailError is CreateAccountEmailViewState.EmailError.None,
                error = emailError
            )
            if (emailState.termsAccepted) onTermsAccept(serverConfig)
        }
    }

    final override fun onTermsAccept(serverConfig: ServerConfig) {
        emailState = emailState.copy(loading = true, continueEnabled = false, termsDialogVisible = false, termsAccepted = true)
        viewModelScope.launch {
            val emailError = requestActivationCodeUseCase(emailState.email.text.trim().lowercase(), serverConfig).toEmailError()
            emailState = emailState.copy(loading = false, continueEnabled = true, error = emailError)
            if (emailError is CreateAccountEmailViewState.EmailError.None) onTermsSuccess()
        }
    }

    final override fun onTermsDialogDismiss() {
        emailState = emailState.copy(termsDialogVisible = false)
    }

    abstract fun onTermsSuccess()
    final override fun openLogin() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.Login.getRouteWithArgs(),
                    BackStackMode.CLEAR_TILL_START
                )
            )
        }
    }

    // Details
    final override fun onDetailsChange(newText: TextFieldValue, fieldType: CreateAccountDetailsViewModel.DetailsFieldType) {
        detailsState = when (fieldType) {
            CreateAccountDetailsViewModel.DetailsFieldType.FirstName -> detailsState.copy(firstName = newText)
            CreateAccountDetailsViewModel.DetailsFieldType.LastName -> detailsState.copy(lastName = newText)
            CreateAccountDetailsViewModel.DetailsFieldType.Password -> detailsState.copy(password = newText)
            CreateAccountDetailsViewModel.DetailsFieldType.ConfirmPassword -> detailsState.copy(confirmPassword = newText)
            CreateAccountDetailsViewModel.DetailsFieldType.TeamName -> detailsState.copy(teamName = newText)
        }.let {
            it.copy(
                error = CreateAccountDetailsViewState.DetailsError.None,
                continueEnabled = it.fieldsNotEmpty() && !it.loading
            )
        }
    }

    final override fun onDetailsErrorDismiss() {
        detailsState = detailsState.copy(error = CreateAccountDetailsViewState.DetailsError.None)
    }

    final override fun onDetailsContinue(serverConfig: ServerConfig) {
        detailsState = detailsState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val detailsError = when {
                !validatePasswordUseCase(detailsState.password.text) ->
                    CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError
                detailsState.password.text != detailsState.confirmPassword.text ->
                    CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError
                else -> CreateAccountDetailsViewState.DetailsError.None
            }
            detailsState = detailsState.copy(
                loading = false,
                continueEnabled = true,
                error = detailsError
            )
            if (detailsState.error is CreateAccountDetailsViewState.DetailsError.None) onDetailsSuccess()
        }
    }

    abstract fun onDetailsSuccess()

    // Code
    final override fun onCodeChange(newValue: CodeFieldValue, serverConfig: ServerConfig) {
        codeState = codeState.copy(code = newValue, error = CreateAccountCodeViewState.CodeError.None)
        if (newValue.isFullyFilled) onCodeContinue(serverConfig)
    }

    final override fun onCodeErrorDismiss() {
        codeState = codeState.copy(error = CreateAccountCodeViewState.CodeError.None)
    }

    final override fun resendCode(serverConfig: ServerConfig) {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {
            val codeError = requestActivationCodeUseCase(emailState.email.text.trim().lowercase(), serverConfig).toCodeError()
            codeState = codeState.copy(loading = false, error = codeError)
        }
    }

    private fun onCodeContinue(serverConfig: ServerConfig) {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {

            val registerParam = when (type) {
                CreateAccountFlowType.CreatePersonalAccount ->
                    RegisterParam.PrivateAccount(
                        firstName = detailsState.firstName.text.trim(),
                        lastName = detailsState.lastName.text.trim(),
                        password = detailsState.password.text,
                        email = emailState.email.text.trim().lowercase(),
                        emailActivationCode = codeState.code.text.text
                    )
                CreateAccountFlowType.CreateTeam ->
                    RegisterParam.Team(
                        firstName = detailsState.firstName.text.trim(),
                        lastName = detailsState.lastName.text.trim(),
                        password = detailsState.password.text,
                        email = emailState.email.text.trim().lowercase(),
                        emailActivationCode = codeState.code.text.text,
                        teamName = detailsState.teamName.text.trim(),
                        teamIcon = "default"
                    )
            }

            val (userInfo, session) = registerAccountUseCase(registerParam, serverConfig).let {
                when (it) {
                    is RegisterResult.Failure -> {
                        updateCodeErrorState(it.toCodeError())
                        return@launch
                    }
                    is RegisterResult.Success -> it.value
                }
            }
            val storedUserId = addAuthenticatedUser(session, false).let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        updateCodeErrorState(it.toCodeError())
                        return@launch
                    }
                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            registerClient(storedUserId, registerParam.password).let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        updateCodeErrorState(it.toCodeError())
                        return@launch
                    }
                    is RegisterClientResult.Success -> {
                        registerPushToken(storedUserId, it.client.clientId.value)
                        onCodeSuccess()
                    }
                }
            }
        }
    }

    private fun updateCodeErrorState(codeError: CreateAccountCodeViewState.CodeError) {
        codeState = if (codeError is CreateAccountCodeViewState.CodeError.None) {
            codeState.copy(error = codeError)

        } else {
            codeState.copy(loading = false, error = codeError)
        }
    }

    private suspend fun registerClient(userId: UserId, password: String) =
        clientScopeProviderFactory.create(userId).clientScope.register(
            RegisterClientParam(
                password = password,
                capabilities = null
            )
        )

    private suspend fun registerPushToken(userId: UserId, clientId: String) {
        clientScopeProviderFactory.create(userId).clientScope.registerPushToken(BuildConfig.SENDER_ID, clientId)
            .let { registerTokenResult ->
                when (registerTokenResult) {
                    is RegisterTokenResult.Success ->
                        appLogger.i("PushToken Registered Successfully")
                    is RegisterTokenResult.Failure ->
                        //TODO: handle failure in settings to allow the user to retry tokenRegistration
                        appLogger.i("PushToken Registration Failed: $registerTokenResult")
                }
            }
    }


    abstract fun onCodeSuccess()
    final override fun onTooManyDevicesError() {
        codeState = codeState.copy(
            code = CodeFieldValue(text = TextFieldValue(""), isFullyFilled = false),
            error = CreateAccountCodeViewState.CodeError.None
        )
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }
}

private fun RequestActivationCodeResult.toEmailError() = when (this) {
    RequestActivationCodeResult.Failure.AlreadyInUse -> CreateAccountEmailViewState.EmailError.TextFieldError.AlreadyInUseError
    RequestActivationCodeResult.Failure.BlacklistedEmail -> CreateAccountEmailViewState.EmailError.TextFieldError.BlacklistedEmailError
    RequestActivationCodeResult.Failure.DomainBlocked -> CreateAccountEmailViewState.EmailError.TextFieldError.DomainBlockedError
    RequestActivationCodeResult.Failure.InvalidEmail -> CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError
    is RequestActivationCodeResult.Failure.Generic -> CreateAccountEmailViewState.EmailError.DialogError.GenericError(this.failure)
    RequestActivationCodeResult.Success -> CreateAccountEmailViewState.EmailError.None
}

private fun RequestActivationCodeResult.toCodeError() = when (this) {
    RequestActivationCodeResult.Failure.AlreadyInUse -> CreateAccountCodeViewState.CodeError.DialogError.AccountAlreadyExistsError
    RequestActivationCodeResult.Failure.BlacklistedEmail -> CreateAccountCodeViewState.CodeError.DialogError.BlackListedError
    RequestActivationCodeResult.Failure.DomainBlocked -> CreateAccountCodeViewState.CodeError.DialogError.EmailDomainBlockedError
    RequestActivationCodeResult.Failure.InvalidEmail -> CreateAccountCodeViewState.CodeError.DialogError.InvalidEmailError
    is RequestActivationCodeResult.Failure.Generic -> CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.failure)
    RequestActivationCodeResult.Success -> CreateAccountCodeViewState.CodeError.None
}

private fun RegisterClientResult.Failure.toCodeError() = when (this) {
    RegisterClientResult.Failure.TooManyClients -> CreateAccountCodeViewState.CodeError.TooManyDevicesError
    RegisterClientResult.Failure.InvalidCredentials -> CreateAccountCodeViewState.CodeError.DialogError.InvalidEmailError
    is RegisterClientResult.Failure.Generic -> CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.genericFailure)
}

private fun RegisterResult.Failure.toCodeError() = when (this) {
    RegisterResult.Failure.InvalidActivationCode -> CreateAccountCodeViewState.CodeError.TextFieldError.InvalidActivationCodeError
    RegisterResult.Failure.AccountAlreadyExists -> CreateAccountCodeViewState.CodeError.DialogError.AccountAlreadyExistsError
    RegisterResult.Failure.BlackListed -> CreateAccountCodeViewState.CodeError.DialogError.BlackListedError
    RegisterResult.Failure.EmailDomainBlocked -> CreateAccountCodeViewState.CodeError.DialogError.EmailDomainBlockedError
    RegisterResult.Failure.InvalidEmail -> CreateAccountCodeViewState.CodeError.DialogError.InvalidEmailError
    RegisterResult.Failure.TeamMembersLimitReached -> CreateAccountCodeViewState.CodeError.DialogError.TeamMembersLimitError
    RegisterResult.Failure.UserCreationRestricted -> CreateAccountCodeViewState.CodeError.DialogError.CreationRestrictedError
    is RegisterResult.Failure.Generic -> CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.failure)
}

private fun AddAuthenticatedUserUseCase.Result.Failure.toCodeError() = when (this) {
    is AddAuthenticatedUserUseCase.Result.Failure.Generic ->
        CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> CreateAccountCodeViewState.CodeError.DialogError.UserAlreadyExists
}
