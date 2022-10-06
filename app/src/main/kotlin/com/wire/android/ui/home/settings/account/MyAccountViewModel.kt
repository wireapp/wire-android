package com.wire.android.ui.home.settings.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MyAccountViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val getSelfTeam: GetSelfTeamUseCase,
    private val serverConfig: SelfServerConfigUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    var myAccountState by mutableStateOf(MyAccountState())
        private set

    init {
        viewModelScope.launch {
            fetchSelfUser()
            loadPasswordChangeContextIfPossible()
        }
    }

    private suspend fun loadPasswordChangeContextIfPossible() {
        viewModelScope.launch {
            when (val result = withContext(dispatchers.io()) { isPasswordRequired() }) {
                is IsPasswordRequiredUseCase.Result.Failure -> appLogger.e("Error when fetching if user can change password")
                is IsPasswordRequiredUseCase.Result.Success -> {
                    when (result.value) {
                        true -> fetchChangePasswordUrl()
                        false -> appLogger.i("The current user has SSO identity, so it does not require password")
                    }
                }
            }
        }
    }

    private suspend fun fetchChangePasswordUrl() {
        when (val result = withContext(dispatchers.io()) { serverConfig() }) {
            is SelfServerConfigUseCase.Result.Failure -> appLogger.e("Error when fetching the accounts url for change password")
            is SelfServerConfigUseCase.Result.Success -> myAccountState =
                myAccountState.copy(changePasswordUrl = result.serverLinks.links.forgotPassword)
        }
    }

    private suspend fun fetchSelfUser() {
        viewModelScope.launch {
            val self = getSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
            val selfTeam = getSelfTeam().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))

            combine(self, selfTeam) { selfUser: SelfUser, team: Team? -> selfUser to team }
                .collect { (user, team) ->
                    myAccountState = myAccountState.copy(
                        fullName = user.name.orEmpty(),
                        userName = user.handle.orEmpty(),
                        email = user.email.orEmpty(),
                        teamName = team?.name.orEmpty(),
                        domain = user.id.domain
                    )
                }
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
