package com.wire.android.ui


import android.content.Intent
import android.util.Log

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wire.android.navigation.NavigationItem
import com.wire.kalium.logic.configuration.GetServerConfigResult
import com.wire.kalium.logic.configuration.GetServerConfigUseCase
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val currentSessionUseCase: CurrentSessionUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor
) : ViewModel() {

    private val currentSession: AuthSession? = runBlocking {
        return@runBlocking when (val result = currentSessionUseCase.invoke()) {
            is CurrentSessionResult.Success -> result.authSession
            else -> null
        }
    }

    private val isUserLoggedIn = currentSession != null
    var serverConfig: ServerConfig = ServerConfig.DEFAULT
    private var ssoDeepLinkResult: DeepLinkResult.SSOLogin? = null

    fun navigationArguments() =
        if (ssoDeepLinkResult != null) {
            listOf(serverConfig, ssoDeepLinkResult!!)
        } else listOf(serverConfig)

    fun startNavigationRoute() = when {
        ssoDeepLinkResult is DeepLinkResult.SSOLogin -> NavigationItem.Login.getRouteWithArgs()
        serverConfig.apiBaseUrl != ServerConfig.DEFAULT.apiBaseUrl -> NavigationItem.Login.getRouteWithArgs()
        isUserLoggedIn -> NavigationItem.Home.getRouteWithArgs()
        else -> NavigationItem.Welcome.getRouteWithArgs()
    }

    private fun loadServerConfig(url: String) = runBlocking {
        return@runBlocking when (val result = getServerConfigUseCase.invoke(url)) {
            is GetServerConfigResult.Success -> result.serverConfig
            else -> ServerConfig.DEFAULT
        }
    }

    fun handleDeepLink(intent: Intent) {
        intent.data?.let {
            with(deepLinkProcessor(it)) {
                when (this) {
                    is DeepLinkResult.CustomServerConfig ->
                        serverConfig = loadServerConfig(url)
                    is DeepLinkResult.SSOLogin ->
                        ssoDeepLinkResult = this
                    DeepLinkResult.Unknown -> TODO()
                }
            }
        }
    }
}
