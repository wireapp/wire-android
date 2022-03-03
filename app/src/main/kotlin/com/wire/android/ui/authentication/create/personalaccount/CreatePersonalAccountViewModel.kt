package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.code.CodeViewModel
import com.wire.android.ui.authentication.create.code.CodeViewState
import com.wire.android.ui.authentication.create.details.DetailsViewModel
import com.wire.android.ui.authentication.create.details.DetailsViewState
import com.wire.android.ui.authentication.create.email.EmailViewModel
import com.wire.android.ui.authentication.create.email.EmailViewState
import com.wire.android.ui.authentication.create.overview.OverviewViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("MagicNumber")
@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreatePersonalAccountViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
) : ViewModel(), OverviewViewModel, EmailViewModel, DetailsViewModel, CodeViewModel {
    var moveToStep = MutableSharedFlow<CreatePersonalAccountNavigationItem>()
    var moveBack = MutableSharedFlow<Unit>()
    override var emailState: EmailViewState by mutableStateOf(EmailViewState())
    override var detailsState: DetailsViewState by mutableStateOf(DetailsViewState())
    override var codeState: CodeViewState by mutableStateOf(CodeViewState())


    // Navigation
    private fun goToStep(item: CreatePersonalAccountNavigationItem) { viewModelScope.launch { moveToStep.emit(item) } }
    fun closeForm() { viewModelScope.launch { navigationManager.navigateBack() } }
    override fun goBackToPreviousStep() { viewModelScope.launch { moveBack.emit(Unit) } }

    // Overview
    override fun onOverviewContinue() { goToStep(CreatePersonalAccountNavigationItem.Email) }

    // Email
    override fun onEmailChange(newText: TextFieldValue) {
        emailState = emailState.copy(
                email = newText,
                error = EmailViewState.EmailError.None,
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
    override fun onDetailsChange(newText: TextFieldValue, fieldType: DetailsViewModel.DetailsFieldType) {
        detailsState = when(fieldType) {
            DetailsViewModel.DetailsFieldType.FirstName -> detailsState.copy(firstName = newText)
            DetailsViewModel.DetailsFieldType.LastName -> detailsState.copy(lastName = newText)
            DetailsViewModel.DetailsFieldType.Password -> detailsState.copy(password = newText)
            DetailsViewModel.DetailsFieldType.ConfirmPassword -> detailsState.copy(confirmPassword = newText)
        }.let { it.copy(
                error = DetailsViewState.DetailsError.None,
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
                        DetailsViewState.DetailsError.PasswordsNotMatchingError
                    detailsState.password.text.length < DetailsViewModel.MIN_PASSWORD_LENGTH ->
                        DetailsViewState.DetailsError.InvalidPasswordError
                    else -> DetailsViewState.DetailsError.None
                }
            )
            if(detailsState.error is DetailsViewState.DetailsError.None)
                goToStep(CreatePersonalAccountNavigationItem.Code)
        }
    }

    // Code
    override fun onCodeChange(newValue: TextFieldValue) {
        codeState = codeState.copy(code = newValue, error = CodeViewState.CodeError.None)
        //TODO perform request when code is filled
    }
    override fun onResendCodePressed() {
        //TODO
    }

    override fun onCodeContinue() {
        //TODO
    }
}
