package com.wire.android.ui

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extension.intervalFlow
import com.wire.kalium.logic.configuration.GetServerConfigResult
import com.wire.kalium.logic.configuration.GetServerConfigUseCase
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalMaterial3Api
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val currentSessionUseCase: CurrentSessionUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val notificationManager: WireNotificationManager,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val currentSession: AuthSession? = runBlocking {
        return@runBlocking when (val result = currentSessionUseCase()) {
            is CurrentSessionResult.Success -> result.authSession
            else -> null
        }
    }

    private val isUserLoggedIn = currentSession != null
//    var serverConfig: ServerConfig = ServerConfig.DEFAULT
//    private var ssoDeepLinkResult: DeepLinkResult.SSOLogin? = null

    private val navigationArguments = mutableMapOf<String, Any>()
        .apply { put(SERVER_CONFIG_ARG, ServerConfig.DEFAULT) }

    fun navigationArguments() =
//        if (ssoDeepLinkResult != null) {
//            listOf(serverConfig, ssoDeepLinkResult!!)
//        } else listOf(serverConfig)
        navigationArguments.values.toList()

    fun startNavigationRoute() = when {
        shouldStartFromLogin() -> NavigationItem.Login.getRouteWithArgs()
        shouldStartFromIncomingCall() -> NavigationItem.IncomingCall.getRouteWithArgs()
        shouldStartFromHome() -> NavigationItem.Home.getRouteWithArgs()
//        ssoDeepLinkResult is DeepLinkResult.SSOLogin -> NavigationItem.Login.getRouteWithArgs()
//        serverConfig.apiBaseUrl != ServerConfig.DEFAULT.apiBaseUrl -> NavigationItem.Login.getRouteWithArgs()
//        isUserLoggedIn -> NavigationItem.Home.getRouteWithArgs()
        else -> NavigationItem.Welcome.getRouteWithArgs()
    }

    fun handleDeepLink(intent: Intent) {
        println("cyka deeplink: ${intent.data}")
        intent.data?.let {
            with(deepLinkProcessor(it)) {
                when (this) {
                    is DeepLinkResult.CustomServerConfig ->
//                        serverConfig = loadServerConfig(url)
                        navigationArguments.put(SERVER_CONFIG_ARG, loadServerConfig(url))
                    is DeepLinkResult.SSOLogin ->
//                        ssoDeepLinkResult = this
                        navigationArguments.put(SSO_DEEPLINK_ARG, this)
                    is DeepLinkResult.IncomingCall ->
                        navigationArguments.put(INCOMING_CALL_CONVERSATION_ID_ARG, this.conversationsId)
                    DeepLinkResult.Unknown -> TODO()
                }
            }
        }
    }

    init {
        println("cyla init ViewModel")
        // checking CurrentSession every minute, to subscribe/unsubscribe from the notifications
        // according ot UserId changes
        // TODO this intervalFlow is a temporary solution to have updated UserId,
        // waiting for refactoring in kalium
        val getUserIdFlow = intervalFlow(CHECK_USER_ID_FREQUENCY_MS)
            .map {
                when (val result = currentSessionUseCase()) {
                    is CurrentSessionResult.Success -> result.authSession.userId
                    else -> null
                }
            }
            // do nothing if UserId wasn't changed
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

        viewModelScope.launch { notificationManager.observeMessageNotifications(getUserIdFlow) }
        viewModelScope.launch {
            notificationManager.observeIncomingCalls(getUserIdFlow) { /*goToIncomingCall(it.conversationId)*/ }
        }
    }

    private fun loadServerConfig(url: String) = runBlocking {
        return@runBlocking when (val result = getServerConfigUseCase(url)) {
            is GetServerConfigResult.Success -> result.serverConfig
            else -> ServerConfig.DEFAULT
        }
    }

    private suspend fun goToIncomingCall(conversationId: ConversationId) {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))
            )
        )
    }

    private fun shouldStartFromLogin(): Boolean =
        (navigationArguments[SERVER_CONFIG_ARG] as ServerConfig).apiBaseUrl != ServerConfig.DEFAULT.apiBaseUrl ||
                navigationArguments[SSO_DEEPLINK_ARG] != null

    private fun shouldStartFromHome(): Boolean  = isUserLoggedIn

    private fun shouldStartFromIncomingCall(): Boolean =
        (navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as? ConversationId) != null

    companion object {
        private const val CHECK_USER_ID_FREQUENCY_MS = 60_000L

        private const val SERVER_CONFIG_ARG = "server_config"
        private const val SSO_DEEPLINK_ARG = "sso_deeplink"
        private const val INCOMING_CALL_CONVERSATION_ID_ARG = "incoming_call_conversation_id"
    }
}
