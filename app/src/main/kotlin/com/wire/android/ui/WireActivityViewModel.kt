package com.wire.android.ui


import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import com.wire.android.WireApplication
import com.wire.android.navigation.NavigationItem
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.configuration.GetServerConfigUseCase
import com.wire.kalium.logic.configuration.GetServerConfigResult
import com.wire.kalium.logic.configuration.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val currentSessionUseCase: CurrentSessionUseCase
) : ViewModel() {

    private val currentSession: AuthSession? = runBlocking {
        return@runBlocking when (val result = currentSessionUseCase.invoke()) {
            is CurrentSessionResult.Success -> result.authSession
            else -> null
        }
    }

    private val isUserLoggedIn = currentSession != null

    val startNavigationRoute = when {
        WireApplication.serverConfig.apiBaseUrl != ServerConfig.STAGING.apiBaseUrl -> NavigationItem.Login.getRouteWithArgs()
        isUserLoggedIn -> NavigationItem.Home.getRouteWithArgs()
        else -> NavigationItem.Welcome.getRouteWithArgs()
    }

//    fun loadServerConfig(url: String) = runBlocking {
//        return@runBlocking when (val result = getServerConfigUserCase.invoke(url)) {
//            is GetServerConfigResult.Success -> result.serverConfig
//            else -> ServerConfig.STAGING
//        }
//    }

    fun handleDeepLink(intent: Intent) {
        intent.data?.getQueryParameter(SERVER_CONFIG_DEEPLINK)?.let {
            WireApplication.serverConfig = ServerConfig.STAGING
        }
    }

    companion object {
        const val SERVER_CONFIG_DEEPLINK = "config"
    }
}
