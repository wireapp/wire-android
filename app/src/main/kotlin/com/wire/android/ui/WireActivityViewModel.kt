package com.wire.android.ui

import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    dispatchers: DispatcherProvider,
    currentSessionFlow: CurrentSessionFlowUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val notificationManager: WireNotificationManager,
    private val navigationManager: NavigationManager,
    private val authServerConfigProvider: AuthServerConfigProvider
) : ViewModel(), DefaultLifecycleObserver {

    private val isAppVisibleFlow = MutableStateFlow(true)
    private val navigationArguments = mutableMapOf<String, Any>(SERVER_CONFIG_ARG to ServerConfig.DEFAULT)

    private val observeUserId = currentSessionFlow()
        .map { result ->
            if (result is CurrentSessionResult.Success) result.authSession.session.userId
            else {
                //get the reason
                navigationManager.navigate(
                    NavigationCommand(
                        NavigationItem.Welcome.getRouteWithArgs(),
                        BackStackMode.CLEAR_WHOLE
                    )
                )
                null
            }
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    init {
        viewModelScope.launch {
            launch { notificationManager.observeMessageNotifications(observeUserId) }
            launch {
                notificationManager.observeIncomingCalls(
                    isAppVisibleFlow,
                    observeUserId
                ) { openIncomingCall(it.conversationId) }
            }
        }
    }

    fun navigationArguments() = navigationArguments.values.toList()

    fun startNavigationRoute(): String =
        when {
            shouldGoToLogin() -> NavigationItem.Login.getRouteWithArgs()
            shouldGoToWelcome() -> NavigationItem.Welcome.getRouteWithArgs()
            shouldGoToIncomingCall() -> NavigationItem.IncomingCall.getRouteWithArgs()
            shouldGoToConversation() -> NavigationItem.Conversation.getRouteWithArgs()
            else -> NavigationItem.Home.getRouteWithArgs()
        }

    fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { deepLink ->
            when (val result = deepLinkProcessor(deepLink)) {
                is DeepLinkResult.CustomServerConfig ->
                    loadServerConfig(result.url)?.let { serverLinks ->
                        authServerConfigProvider.updateAuthServer(serverLinks)
                        navigationArguments.put(SERVER_CONFIG_ARG, serverLinks)
                    }

                is DeepLinkResult.SSOLogin ->
                    navigationArguments.put(SSO_DEEPLINK_ARG, result)

                is DeepLinkResult.IncomingCall -> {
                    if (isLaunchedFromHistory(intent)) {
                        //We don't need to handle deepLink, if activity was launched from history.
                        //For example: user opened app by deepLink, then closed it by back button click,
                        //then open the app from the "Recent Apps"
                        appLogger.i("IncomingCall deepLink launched from the history")
                    } else {
                        navigationArguments.put(INCOMING_CALL_CONVERSATION_ID_ARG, result.conversationsId)
                    }
                }
                is DeepLinkResult.OpenConversation -> {
                    if (isLaunchedFromHistory(intent)) {
                        appLogger.i("OpenConversation deepLink launched from the history")
                    } else {
                        navigationArguments.put(OPEN_CONVERSATION_ID_ARG, result.conversationsId)
                    }
                }

                DeepLinkResult.Unknown -> {
                    appLogger.e("unknown deeplink result $result")
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
            remove(OPEN_CONVERSATION_ID_ARG)
            remove(SSO_DEEPLINK_ARG)
        }

        handleDeepLink(intent)

        return when {
            shouldGoToLogin() || shouldGoToWelcome() -> true
            shouldGoToIncomingCall() -> {
                openIncomingCall(navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as ConversationId)
                false
            }
            shouldGoToConversation() -> {
                openConversation(navigationArguments[OPEN_CONVERSATION_ID_ARG] as ConversationId)
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

    private fun openConversation(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.Conversation.getRouteWithArgs(listOf(conversationId)), BackStackMode.UPDATE_EXISTED))
    }

    private fun isLaunchedFromHistory(intent: Intent?) =
        intent?.flags != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch {
            navigationManager.navigate(command)
        }
    }

    private fun loadServerConfig(url: String): ServerConfig.Links? = runBlocking {
        return@runBlocking when (val result = getServerConfigUseCase(url)) {
            is GetServerConfigResult.Success -> result.serverConfig.links
            // TODO: show error message on failure
            is GetServerConfigResult.Failure.Generic -> {
                appLogger.e("something went wrong during handling the scustom server deep link: ${result.genericFailure}")
                null
            }

            GetServerConfigResult.Failure.TooNewVersion -> {
                appLogger.e("server version is too new")
                null
            }

            GetServerConfigResult.Failure.UnknownServerVersion -> {
                appLogger.e("unknown server version")
                null
            }
        }
    }

    private fun shouldGoToLogin(): Boolean =
        (navigationArguments[SERVER_CONFIG_ARG] as ServerConfig.Links) != ServerConfig.DEFAULT ||
                navigationArguments[SSO_DEEPLINK_ARG] != null

    private fun shouldGoToIncomingCall(): Boolean =
        (navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToConversation(): Boolean =
        (navigationArguments[OPEN_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToWelcome(): Boolean = runBlocking { observeUserId.first() } == null

    companion object {
        private const val SERVER_CONFIG_ARG = "server_config"
        private const val SSO_DEEPLINK_ARG = "sso_deeplink"
        private const val INCOMING_CALL_CONVERSATION_ID_ARG = "incoming_call_conversation_id"
        private const val OPEN_CONVERSATION_ID_ARG = "open_conversation_id"
    }
}
