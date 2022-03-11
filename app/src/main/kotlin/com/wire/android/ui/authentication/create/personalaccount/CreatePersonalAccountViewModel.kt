package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.CreateAccountFlowType
import com.wire.android.ui.authentication.create.CreateAccountUsernameFlowType
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewState
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewState
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewState
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.common.textfield.CodeFieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("MagicNumber", "TooManyFunctions")
@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreatePersonalAccountViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
) : ViewModel(), CreateAccountOverviewViewModel, CreateAccountEmailViewModel, CreateAccountDetailsViewModel, CreateAccountCodeViewModel {
    var moveToStep = MutableSharedFlow<CreatePersonalAccountNavigationItem>()
    var moveBack = MutableSharedFlow<Unit>()
    override var emailState: CreateAccountEmailViewState by mutableStateOf(CreateAccountEmailViewState())
    override var detailsState: CreateAccountDetailsViewState by mutableStateOf(CreateAccountDetailsViewState())
    override var codeState: CreateAccountCodeViewState by mutableStateOf(CreateAccountCodeViewState())


    // Navigation
    private fun goToStep(item: CreatePersonalAccountNavigationItem) { viewModelScope.launch { moveToStep.emit(item) } }
    fun closeForm() { viewModelScope.launch { navigationManager.navigateBack() } }
    override fun goBackToPreviousStep() { viewModelScope.launch { moveBack.emit(Unit) } }

    private fun clearState() {
        emailState = CreateAccountEmailViewState()
        detailsState = CreateAccountDetailsViewState()
        codeState = CreateAccountCodeViewState()
    }

    // Overview
    override fun onOverviewContinue() {
        clearState()
        goToStep(CreatePersonalAccountNavigationItem.Email)
    }

    // Email
    override fun onEmailChange(newText: TextFieldValue) {
        emailState = emailState.copy(
                email = newText,
                error = CreateAccountEmailViewState.EmailError.None,
                continueEnabled = newText.text.isNotEmpty() && !emailState.loading)
        codeState = codeState.copy(email = newText.text)
    }
    override fun onEmailContinue() {
        emailState = emailState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch { //TODO replace with proper logic
            emailState = emailState.copy(loading = false, continueEnabled = true, termsDialogVisible = true)
        }
    }
    override fun onTermsDialogDismiss() { emailState = emailState.copy(termsDialogVisible = false) }
    override fun onTermsAccepted() { goToStep(CreatePersonalAccountNavigationItem.Details) }
    override fun openLogin() {
        viewModelScope.launch { navigationManager.navigate(NavigationCommand(NavigationItem.Login.getRouteWithArgs())) }
    }

    // Details
    override fun onDetailsChange(newText: TextFieldValue, fieldType: CreateAccountDetailsViewModel.DetailsFieldType) {
        detailsState = when(fieldType) {
            CreateAccountDetailsViewModel.DetailsFieldType.FirstName -> detailsState.copy(firstName = newText)
            CreateAccountDetailsViewModel.DetailsFieldType.LastName -> detailsState.copy(lastName = newText)
            CreateAccountDetailsViewModel.DetailsFieldType.Password -> detailsState.copy(password = newText)
            CreateAccountDetailsViewModel.DetailsFieldType.ConfirmPassword -> detailsState.copy(confirmPassword = newText)
        }.let { it.copy(
                error = CreateAccountDetailsViewState.DetailsError.None,
                continueEnabled = it.fieldsNotEmpty() && !it.loading
            )
        }
    }
    override fun onDetailsContinue() {
        detailsState = detailsState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch { //TODO replace with proper logic
            detailsState = detailsState.copy(
                loading = false,
                continueEnabled = true,
                error = when {
                    detailsState.password.text != detailsState.confirmPassword.text ->
                        CreateAccountDetailsViewState.DetailsError.PasswordsNotMatchingError
                    detailsState.password.text.length < CreateAccountDetailsViewModel.MIN_PASSWORD_LENGTH ->
                        CreateAccountDetailsViewState.DetailsError.InvalidPasswordError
                    else -> CreateAccountDetailsViewState.DetailsError.None
                }
            )
            if(detailsState.error is CreateAccountDetailsViewState.DetailsError.None)
                goToStep(CreatePersonalAccountNavigationItem.Code)
        }
    }

    // Code
    override fun onCodeChange(newValue: CodeFieldValue) {
        codeState = codeState.copy(code = newValue.text, error = CreateAccountCodeViewState.CodeError.None)
        if(newValue.isFullyFilled) onCodeContinue()
    }
    override fun resendCode() {
        //TODO
    }

    override fun onCodeContinue() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.CreateUsername.getRouteWithArgs(listOf(CreateAccountUsernameFlowType.CreatePersonalAccount)),
                    BackStackMode.CLEAR_WHOLE
                )
            )
        }
    }
}
