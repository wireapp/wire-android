package com.wire.android.ui

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.GetServerConfigResult
import com.wire.kalium.logic.configuration.GetServerConfigUseCase
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalMaterial3Api::class
)
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    dispatchers: DispatcherProvider,
    currentSessionFlow: CurrentSessionFlowUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val notificationManager: WireNotificationManager,
    private val navigationManager: NavigationManager
) : ViewModel(), DefaultLifecycleObserver {

    private val isAppVisibleFlow = MutableStateFlow(true)
    private val navigationArguments = mutableMapOf<String, Any>(SERVER_CONFIG_ARG to ServerConfig.DEFAULT)

    private val userIdFlow = currentSessionFlow()
        .map { result ->
            if (result is CurrentSessionResult.Success) result.authSession.userId
            else null
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    init {
        viewModelScope.launch {
            launch { notificationManager.observeMessageNotifications(userIdFlow) }
            launch {
                notificationManager.observeIncomingCalls(
                    isAppVisibleFlow,
                    userIdFlow
                ) { openIncomingCall(it.conversationId) }
            }
            launch {
                userIdFlow
                    .filterNotNull()
                    .collect { userId ->
                        // listen for the WebSockets updates and update DB accordingly
                        launch { coreLogic.getSessionScope(userId).listenToEvents() }
                    }
            }
        }
    }

    fun navigationArguments() = navigationArguments.values.toList()

    fun startNavigationRoute(): String =
        when {
            shouldGoToLogin() -> NavigationItem.Login.getRouteWithArgs()
            shouldGoToWelcome() -> NavigationItem.Welcome.getRouteWithArgs()
            shouldGoToIncomingCall() -> NavigationItem.IncomingCall.getRouteWithArgs()
            else -> NavigationItem.Home.getRouteWithArgs()
        }

    fun handleDeepLink(intent: Intent?) {
        intent?.data?.let {
            val result = deepLinkProcessor(it)
            with(result) {
                when (this) {
                    is DeepLinkResult.CustomServerConfig ->
                        navigationArguments.put(SERVER_CONFIG_ARG, loadServerConfig(url))
                    is DeepLinkResult.SSOLogin ->
                        navigationArguments.put(SSO_DEEPLINK_ARG, this)
                    is DeepLinkResult.IncomingCall ->
                        navigationArguments.put(INCOMING_CALL_CONVERSATION_ID_ARG, this.conversationsId)
                    DeepLinkResult.Unknown -> TODO()
                }
            }
        }
    }

    /**
     * Some of the deepLinks require to recreate Activity (Login, Welcome, etc.)
     * Others need to just open some screen, without recreating (Conversation, IncomingCall, etc.)
     *
     * @return true if Activity needs to be recreated, false - otherwise
     */
    fun handleDeepLinkOnNewIntent(intent: Intent?): Boolean {

        //removing arguments that could be there from prev deeplink handling
        navigationArguments.apply {
            remove(INCOMING_CALL_CONVERSATION_ID_ARG)
            remove(SSO_DEEPLINK_ARG)
        }

        handleDeepLink(intent)

        return when {
            shouldGoToLogin() || shouldGoToWelcome() -> true
            shouldGoToIncomingCall() -> {
                openIncomingCall(navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as ConversationId)
                false
            }
            intent == null -> false
            else -> true
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isAppVisibleFlow.value = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isAppVisibleFlow.value = false
    }

    private fun openIncomingCall(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))))
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch {
            navigationManager.navigate(command)
        }
    }

    private fun loadServerConfig(url: String) = runBlocking {
        return@runBlocking when (val result = getServerConfigUseCase(url)) {
            is GetServerConfigResult.Success -> result.serverConfig
            else -> ServerConfig.DEFAULT
        }
    }

    private fun shouldGoToLogin(): Boolean =
        (navigationArguments[SERVER_CONFIG_ARG] as ServerConfig).apiBaseUrl != ServerConfig.DEFAULT.apiBaseUrl ||
                navigationArguments[SSO_DEEPLINK_ARG] != null

    private fun shouldGoToIncomingCall(): Boolean =
        (navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToWelcome(): Boolean = runBlocking { userIdFlow.first() } == null

    companion object {
        private const val SERVER_CONFIG_ARG = "server_config"
        private const val SSO_DEEPLINK_ARG = "sso_deeplink"
        private const val INCOMING_CALL_CONVERSATION_ID_ARG = "incoming_call_conversation_id"
    }
}
