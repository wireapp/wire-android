package com.wire.android.ui.authentication.create.team

import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.common.CreateAccountBaseViewModel
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.register.RegisterAccountUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
@HiltViewModel
class CreateTeamViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    validateEmailUseCase: ValidateEmailUseCase,
    validatePasswordUseCase: ValidatePasswordUseCase,
    requestActivationCodeUseCase: RequestActivationCodeUseCase,
    registerAccountUseCase: RegisterAccountUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory
) : CreateAccountBaseViewModel(
    CreateAccountFlowType.CreateTeam,
    navigationManager,
    validateEmailUseCase,
    validatePasswordUseCase,
    requestActivationCodeUseCase,
    registerAccountUseCase,
    clientScopeProviderFactory
) {
    var moveToStep = MutableSharedFlow<CreateTeamNavigationItem>()
    var moveBack = MutableSharedFlow<Unit>()

    // Navigation
    private fun goToStep(item: CreateTeamNavigationItem) {
        viewModelScope.launch { moveToStep.emit(item) }
    }

    override fun goBackToPreviousStep() {
        viewModelScope.launch { moveBack.emit(Unit) }
    }

    override fun onOverviewSuccess() {
        goToStep(CreateTeamNavigationItem.Email)
    }

    override fun onTermsSuccess() {
        goToStep(CreateTeamNavigationItem.Details)
    }

    override fun onDetailsSuccess() {
        goToStep(CreateTeamNavigationItem.Code)
    }

    override fun onCodeSuccess() {
        goToStep(CreateTeamNavigationItem.Summary)
    }

    override fun onSummarySuccess() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(NavigationItem.CreateUsername.getRouteWithArgs(), BackStackMode.CLEAR_TILL_START)
            )
        }
    }
}
