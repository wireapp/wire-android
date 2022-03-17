package com.wire.android.ui.authentication.create.common

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
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewState
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewState
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewState
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModel
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewState
import com.wire.android.ui.common.textfield.CodeFieldValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
@OptIn(ExperimentalMaterialApi::class)
abstract class CreateAccountBaseViewModel(
    private val navigationManager: NavigationManager,
    final override val type: CreateAccountFlowType
) : ViewModel(),
    CreateAccountOverviewViewModel,
    CreateAccountEmailViewModel,
    CreateAccountDetailsViewModel,
    CreateAccountCodeViewModel,
    CreateAccountSummaryViewModel
{
    override var emailState: CreateAccountEmailViewState by mutableStateOf(CreateAccountEmailViewState(type))
    override var detailsState: CreateAccountDetailsViewState by mutableStateOf(CreateAccountDetailsViewState(type))
    override var codeState: CreateAccountCodeViewState by mutableStateOf(CreateAccountCodeViewState(type))
    override val summaryState: CreateAccountSummaryViewState by mutableStateOf(CreateAccountSummaryViewState(type))
    override val hideKeyboard: MutableSharedFlow<Unit> = MutableSharedFlow()

    fun closeForm() { viewModelScope.launch { navigationManager.navigateBack() } }

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
    final override fun onEmailContinue() {
        emailState = emailState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch { //TODO replace with proper logic
            emailState = emailState.copy(loading = false, continueEnabled = true, termsDialogVisible = true)
        }
    }
    final override fun onTermsAccept() {
        onTermsDialogDismiss()
        onTermsSuccess()
    }
    final override fun onTermsDialogDismiss() { emailState = emailState.copy(termsDialogVisible = false) }
    abstract fun onTermsSuccess()
    final override fun openLogin() {
        viewModelScope.launch { navigationManager.navigate(NavigationCommand(
            NavigationItem.Login.getRouteWithArgs(),
            BackStackMode.CLEAR_TILL_START))
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
    final override fun onDetailsContinue() {
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
            if(detailsState.error is CreateAccountDetailsViewState.DetailsError.None) onDetailsSuccess()
        }
    }
    abstract fun onDetailsSuccess()

    // Code
    final override fun onCodeChange(newValue: CodeFieldValue) {
        codeState = codeState.copy(code = newValue.text, error = CreateAccountCodeViewState.CodeError.None)
        if (newValue.isFullyFilled) onCodeContinue()
    }
    override fun resendCode() {/* TODO */ }
    final override fun onCodeContinue() {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch { //TODO replace with proper logic
            val codeError =
                if(codeState.code.text == "111111") CreateAccountCodeViewState.CodeError.None
                else CreateAccountCodeViewState.CodeError.InvalidCodeError
            codeState = codeState.copy(loading = false, error = codeError)
            if(codeError is CreateAccountCodeViewState.CodeError.None) {
                hideKeyboard.emit(Unit)
                onCodeSuccess()
            }
        }
    }
    abstract fun onCodeSuccess()

    override fun onSummaryContinue() { onSummarySuccess() }
    abstract fun onSummarySuccess()
}
