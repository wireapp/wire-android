package com.wire.android.ui.authentication.create.personalaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.common.CreateAccountBaseViewModel
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.server.FetchApiVersionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class CreatePersonalAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    validateEmailUseCase: ValidateEmailUseCase,
    validatePasswordUseCase: ValidatePasswordUseCase,
    authScope: AutoVersionAuthScopeUseCase,
    addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    authServerConfigProvider: AuthServerConfigProvider,
    fetchApiVersion: FetchApiVersionUseCase
) : CreateAccountBaseViewModel(
    CreateAccountFlowType.CreatePersonalAccount,
    savedStateHandle,
    navigationManager,
    validateEmailUseCase,
    validatePasswordUseCase,
    authScope,
    addAuthenticatedUserUseCase,
    clientScopeProviderFactory,
    authServerConfigProvider,
    fetchApiVersion
) {
    var moveToStep = MutableSharedFlow<CreatePersonalAccountNavigationItem>()
    var moveBack = MutableSharedFlow<Unit>()

    private fun goToStep(item: CreatePersonalAccountNavigationItem) {
        viewModelScope.launch { moveToStep.emit(item) }
    }

    override fun goBackToPreviousStep() {
        viewModelScope.launch { moveBack.emit(Unit) }
    }

    override fun onOverviewSuccess() {
        goToStep(CreatePersonalAccountNavigationItem.Email)
    }

    override fun onTermsSuccess() {
        goToStep(CreatePersonalAccountNavigationItem.Details)
    }

    override fun onDetailsSuccess() {
        goToStep(CreatePersonalAccountNavigationItem.Code)
    }

    override fun onCodeSuccess() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.CreateAccountSummary.getRouteWithArgs(listOf(CreateAccountFlowType.CreatePersonalAccount)),
                    BackStackMode.CLEAR_WHOLE
                )
            )
        }
    }
}
