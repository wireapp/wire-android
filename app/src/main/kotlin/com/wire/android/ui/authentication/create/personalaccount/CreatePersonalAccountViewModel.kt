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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("MagicNumber")
@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreatePersonalAccountViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
) : ViewModel() {
    var moveToStep = MutableSharedFlow<CreatePersonalAccountNavigationItem>()
    var moveBack = MutableSharedFlow<Unit>()
    var state by mutableStateOf(CreatePersonalAccountViewState())
        private set

    // Navigation
    fun goToStep(item: CreatePersonalAccountNavigationItem) { viewModelScope.launch { moveToStep.emit(item) } }
    fun goBackToPreviousStep() { viewModelScope.launch { moveBack.emit(Unit) } }
    fun closeForm() { viewModelScope.launch { navigationManager.navigateBack() } }
    fun openLogin() { viewModelScope.launch { navigationManager.navigate(NavigationCommand(NavigationItem.Login.getRouteWithArgs())) } }

    // Email
    fun onEmailChange(newText: TextFieldValue) {
        state = state.copy(email = state.email.copy(
                email = newText,
                error = CreatePersonalAccountViewState.EmailError.None,
                continueEnabled = newText.text.isNotEmpty() && !state.email.loading
        ))
    }
    fun onEmailContinue() {
        state = state.copy(email = state.email.copy(loading = true, continueEnabled = false))
        viewModelScope.launch { //TODO replace with proper logic
            state = state.copy(email = state.email.copy(loading = false, continueEnabled = true, termsDialogVisible = true))
        }
    }
    fun onTermsDialogDismiss() { state = state.copy(email = state.email.copy(termsDialogVisible = false)) }
    fun onTermsAccepted() { goToStep(CreatePersonalAccountNavigationItem.Details) }

    // Details
    fun onDetailsChange(newText: TextFieldValue, fieldType: DetailsFieldType) {
        state = state.copy(details = when(fieldType) {
            DetailsFieldType.FirstName -> state.details.copy(firstName = newText)
                DetailsFieldType.LastName -> state.details.copy(lastName = newText)
                DetailsFieldType.Password -> state.details.copy(password = newText)
                DetailsFieldType.ConfirmPassword -> state.details.copy(confirmPassword = newText)
        }.let { it.copy(
                error = CreatePersonalAccountViewState.DetailsError.None,
                continueEnabled = it.fieldsNotEmpty() && !it.loading
            )
        })
    }
    fun onDetailsContinue() {
        state = state.copy(details = state.details.copy(loading = true, continueEnabled = false))
        viewModelScope.launch { //TODO replace with proper logic
            state = state.copy(details = state.details.copy(
                loading = false,
                continueEnabled = true,
                error = when {
                    state.details.password.text != state.details.confirmPassword.text ->
                        CreatePersonalAccountViewState.DetailsError.PasswordsNotMatchingError
                    state.details.password.text.length < MIN_PASSWORD_LENGTH ->
                        CreatePersonalAccountViewState.DetailsError.InvalidPasswordError
                    else -> CreatePersonalAccountViewState.DetailsError.None
                }
            ))
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }
}

enum class DetailsFieldType {
    FirstName, LastName, Password, ConfirmPassword
}
