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
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.server.ObserveServerConfigUseCase
import com.wire.kalium.logic.feature.server.UpdateApiVersionsUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
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

    // TODO: current auth serverConfigId should be stored in DB
    private val authServerConfigIdFlow = MutableStateFlow<String?>(null)
    private val deepLinkResultFlow = MutableSharedFlow<DeepLinkResult?>(replay = 1) // to change state after handling possible deep link

    var state by mutableStateOf<WireActivityState>(WireActivityState.Loading)
        private set

    private fun loadServerConfig(url: String) = runBlocking {
        return@runBlocking when (val result = getServerConfigUseCase(url)) {
            is GetServerConfigResult.Success -> result.serverConfig
            else -> ServerConfig.DEFAULT // TODO: should we inform the user that the server config couldn't be loaded?
        }
    }

    fun handleDeepLink(intent: Intent) {
        viewModelScope.launch {
            val deepLinkResult = intent.data?.let { deepLinkProcessor(it) }
            // TODO: if user is already logged in and tries to change serverConfig, return error message
            if (deepLinkResult is DeepLinkResult.CustomServerConfig) {
                loadServerConfig(deepLinkResult.url).let {
                    authServerConfigIdFlow.emit(it.id)
                }
            }
            deepLinkResultFlow.emit(deepLinkResult)
        }
    }

    init {
        viewModelScope.launch {
            listenForServerConfigApiVersionChanges()
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

    private fun navigationArgumentsCombinedFlow() =
        combine(
            currentSessionFlowUseCase(),
            authServerConfigIdFlow,
            deepLinkResultFlow
        ) { currentSessionResult, authServerConfigId, deepLinkResult ->
            when (currentSessionResult) {
                is CurrentSessionResult.Success -> Triple(currentSessionResult.authSession.serverConfig.id, deepLinkResult, true)
                else -> Triple(authServerConfigId, deepLinkResult, false)
            }
        }.distinctUntilChanged { (oldServerConfigId, oldSsoDeepLinkResult, _), (newServerConfigId, newSsoDeepLinkResult, _) ->
            oldServerConfigId == newServerConfigId && oldSsoDeepLinkResult == newSsoDeepLinkResult
        }

    private suspend fun listenForServerConfigApiVersionChanges() {
        withContext(dispatchers.io()) {
            updateApiVersionsUseCase()

            navigationArgumentsCombinedFlow()
                .flatMapLatest { (serverConfigId, ssoDeepLinkResult, isUserLoggedIn) ->
                    when (val result = observeServerConfigUseCase()) {
                        is ObserveServerConfigUseCase.Result.Success ->
                            result.value.map {
                                getWireActivityState(
                                    it.firstOrNull { serverConfigId == null || it.id == serverConfigId },
                                    ssoDeepLinkResult,
                                    isUserLoggedIn
                                )
                            }
                        else ->
                            flowOf(WireActivityState.ServerVersionNotSupported) // TODO: what if there is a storage error?
                    }
                }
                .distinctUntilChanged { oldState, newState ->
                    // when the navigation is already shown, recompose only if arguments change
                    if (oldState is WireActivityState.NavigationGraph && newState is WireActivityState.NavigationGraph)
                        oldState.navigationArguments == newState.navigationArguments
                    else // otherwise recompose when the state changes to the different one
                        oldState::class == newState::class
                }
                .collect { state = it }
        }
    }

    // TODO: get rid of serverConfig as navigation argument, current auth serverConfig should be passed directly to HttpClient
    private fun navigationArguments(serverConfig: ServerConfig, deepLinkResult: DeepLinkResult?) =
        deepLinkResult?.let { listOf(serverConfig, it) } ?: listOf(serverConfig)

    private fun startNavigationRoute(ssoDeepLinkResult: DeepLinkResult?, isUserLoggedIn: Boolean) = when {
        ssoDeepLinkResult is DeepLinkResult.SSOLogin -> NavigationItem.Login.getRouteWithArgs()
        isUserLoggedIn -> NavigationItem.Home.getRouteWithArgs()
        else -> NavigationItem.Welcome.getRouteWithArgs()
    }

    private fun getWireActivityState(serverConfig: ServerConfig?, deepLinkResult: DeepLinkResult?, isUserLoggedIn: Boolean) =
        serverConfig?.let {
            when (serverConfig.commonApiVersion) {
                is CommonApiVersionType.Valid ->
                    WireActivityState.NavigationGraph(
                        startNavigationRoute(deepLinkResult, isUserLoggedIn),
                        navigationArguments(serverConfig, deepLinkResult)
                    )
                CommonApiVersionType.New ->
                    WireActivityState.ClientUpdateRequired("${serverConfig.websiteUrl}/download")
                else ->
                    WireActivityState.ServerVersionNotSupported
            }
        } ?: WireActivityState.ServerVersionNotSupported // TODO: what if null?
}
