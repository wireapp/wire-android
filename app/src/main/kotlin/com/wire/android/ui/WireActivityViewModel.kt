package com.wire.android.ui

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationItem
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extension.intervalFlow
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.server.ObserveServerConfigUseCase
import com.wire.kalium.logic.feature.server.UpdateApiVersionsUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalMaterial3Api
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val currentSessionFlowUseCase: CurrentSessionFlowUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val observeServerConfigUseCase: ObserveServerConfigUseCase,
    private val updateApiVersionsUseCase: UpdateApiVersionsUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val notificationManager: WireNotificationManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private var serverConfig: ServerConfig = ServerConfig.DEFAULT // TODO: unauthorized serverConfigId should be kept in the repository
    private var ssoDeepLinkResult: DeepLinkResult.SSOLogin? = null

    var state by mutableStateOf<WireActivityState>(WireActivityState.Loading)
        private set

    private fun navigationArguments() = ssoDeepLinkResult?.let { listOf(serverConfig, it) } ?: listOf(serverConfig)

    private fun startNavigationRoute(isUserLoggedIn: Boolean) = when {
        ssoDeepLinkResult is DeepLinkResult.SSOLogin -> NavigationItem.Login.getRouteWithArgs()
        serverConfig.apiBaseUrl != ServerConfig.DEFAULT.apiBaseUrl -> NavigationItem.Login.getRouteWithArgs()
        isUserLoggedIn -> NavigationItem.Home.getRouteWithArgs()
        else -> NavigationItem.Welcome.getRouteWithArgs()
    }

    private fun loadServerConfig(url: String) = runBlocking {
        return@runBlocking when (val result = getServerConfigUseCase(url)) {
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

    init {
        viewModelScope.launch {
            listenForServerConfigApiVersioning()
            listenForNotificationsIfPossible()
        }
    }

    private suspend fun listenForNotificationsIfPossible() {
        withContext(dispatchers.io()) {
            val userIdFlow = currentSessionFlowUseCase()
                .map {
                    when (it) {
                        is CurrentSessionResult.Success -> it.authSession.userId
                        else -> null
                    }
                }
                .distinctUntilChanged() // do nothing if UserId wasn't changed

            notificationManager.listenForMessageNotifications(userIdFlow)
        }
    }

    private suspend fun listenForServerConfigApiVersioning() {
        withContext(dispatchers.io()) {
            updateApiVersionsUseCase()
            currentSessionFlowUseCase()
                .flatMapLatest { currentSessionResult ->
                    val (serverConfigId, isUserLoggedIn) = when (currentSessionResult) {
                        is CurrentSessionResult.Success -> currentSessionResult.authSession.serverConfig.id to true
                        else -> serverConfig.id to false // TODO: unauthorized serverConfigId should be taken from the repository
                    }
                    when (val result = observeServerConfigUseCase()) {
                        is ObserveServerConfigUseCase.Result.Success ->
                            result.value.map {
                                getWireActivityState(it.firstOrNull { it.id == serverConfigId }, isUserLoggedIn)
                            }
                        else ->
                            flowOf(WireActivityState.ServerVersionNotSupported) // TODO: what if there is a storage error?
                    }
                }
                .collect { state = it }
        }
    }

    private fun getWireActivityState(serverConfig: ServerConfig?, isUserLoggedIn: Boolean) =
        serverConfig?.let {
            when (serverConfig.commonApiVersion) {
                is CommonApiVersionType.Valid ->
                    WireActivityState.NavigationGraph(startNavigationRoute(isUserLoggedIn), navigationArguments())
                CommonApiVersionType.New ->
                    WireActivityState.ClientUpdateRequired("${serverConfig.websiteUrl}/download")
                else ->
                    WireActivityState.ServerVersionNotSupported
            }
        } ?: WireActivityState.ServerVersionNotSupported // TODO: what if null?
}
