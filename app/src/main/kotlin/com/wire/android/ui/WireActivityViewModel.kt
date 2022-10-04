package com.wire.android.ui

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.common.dialogs.CustomBEDeeplinkDialogState
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    val currentSessionFlow: CurrentSessionFlowUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val notificationManager: WireNotificationManager,
    private val navigationManager: NavigationManager,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase,
) : ViewModel() {

    private val navigationArguments = mutableMapOf<String, Any>(SERVER_CONFIG_ARG to ServerConfig.DEFAULT)
    var globalAppState: GlobalAppState by mutableStateOf(GlobalAppState())
        private set

    private val observeUserId = currentSessionFlow()
        .onEach {
            if (it is CurrentSessionResult.Success) {
                if (it.accountInfo.isValid().not()) {
                    handleInvalidSession((it.accountInfo as AccountInfo.Invalid).logoutReason)
                }
            }
        }
        .map { result ->
            if (result is CurrentSessionResult.Success) {
                if (result.accountInfo.isValid()) {
                    result.accountInfo.userId
                } else {
                    null
                }
            } else {
                null
            }
        }.distinctUntilChanged().flowOn(dispatchers.io()).shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    init {
        viewModelScope.launch(dispatchers.io()) {
            observePersistentWebSocketConnectionStatus().collect {
                if (!it) {
                    notificationManager.observeNotificationsAndCalls(observeUserId, viewModelScope)
                    { openIncomingCall(it.conversationId) }
                }
            }
        }
    }

    private suspend fun handleInvalidSession(logoutReason: LogoutReason) {
        withContext(dispatchers.main()) {
            when (logoutReason) {
                LogoutReason.SELF_SOFT_LOGOUT, LogoutReason.SELF_HARD_LOGOUT -> {
                    // Self logout is handled from the Self user profile screen directly
                }
                LogoutReason.REMOVED_CLIENT ->
                    globalAppState = globalAppState.copy(blockUserUI = CurrentSessionErrorState.RemovedClient)
                LogoutReason.DELETED_ACCOUNT ->
                    globalAppState = globalAppState.copy(blockUserUI = CurrentSessionErrorState.DeletedAccount)
                LogoutReason.SESSION_EXPIRED ->
                    globalAppState = globalAppState.copy(blockUserUI = CurrentSessionErrorState.SessionExpired)
            }
        }
    }

    fun navigateToNextAccountOrWelcome() {
        viewModelScope.launch {
            globalAppState = globalAppState.copy(blockUserUI = null)
            accountSwitch(SwitchAccountParam.SwitchToNextAccountOrWelcome)
        }
    }

    fun navigationArguments() = navigationArguments.values.toList()

    fun startNavigationRoute(): String = when {
        shouldGoToMigration() -> NavigationItem.Migration.getRouteWithArgs()
        shouldGoToWelcome() -> NavigationItem.Welcome.getRouteWithArgs()
        else -> NavigationItem.Home.getRouteWithArgs()
    }

    fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { deepLink ->
            viewModelScope.launch {
                when (val result = deepLinkProcessor(deepLink)) {
                    is DeepLinkResult.CustomServerConfig -> loadServerConfig(result.url)?.let { serverLinks ->
                        globalAppState = globalAppState.copy(
                            customBackendDialog = CustomBEDeeplinkDialogState(
                                shouldShowDialog = true,
                                serverLinks = serverLinks
                            )
                        )
                        navigationArguments.put(SERVER_CONFIG_ARG, serverLinks)
                    }

                    is DeepLinkResult.SSOLogin -> navigationArguments.put(SSO_DEEPLINK_ARG, result)

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

                    is DeepLinkResult.OngoingCall -> {
                        if (isLaunchedFromHistory(intent)) {
                            //We don't need to handle deepLink, if activity was launched from history.
                            //For example: user opened app by deepLink, then closed it by back button click,
                            //then open the app from the "Recent Apps"
                            appLogger.i("IncomingCall deepLink launched from the history")
                        } else {
                            navigationArguments.put(ONGOING_CALL_CONVERSATION_ID_ARG, result.conversationsId)
                        }
                    }

                    is DeepLinkResult.OpenConversation -> {
                        if (isLaunchedFromHistory(intent)) {
                            appLogger.i("OpenConversation deepLink launched from the history")
                        } else {
                            navigationArguments.put(OPEN_CONVERSATION_ID_ARG, result.conversationsId)
                        }
                    }

                    is DeepLinkResult.OpenOtherUserProfile -> {
                        if (isLaunchedFromHistory(intent)) {
                            appLogger.i("OpenOtherUserProfile deepLink launched from the history")
                        } else {
                            navigationArguments.put(OPEN_OTHER_USER_PROFILE_ARG, result.userId)
                        }
                    }

                    DeepLinkResult.Unknown -> {
                        appLogger.e("unknown deeplink result $result")
                    }
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
            remove(ONGOING_CALL_CONVERSATION_ID_ARG)
            remove(OPEN_CONVERSATION_ID_ARG)
            remove(OPEN_OTHER_USER_PROFILE_ARG)
            remove(SSO_DEEPLINK_ARG)
        }

        handleDeepLink(intent)

        return when {
            shouldGoToLogin() || shouldGoToWelcome() || shouldGoToMigration() -> true

            shouldGoToIncomingCall() -> {
                openIncomingCall(navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as ConversationId)
                false
            }

            shouldGoToOngoingCall() -> {
                openOngoingCall(navigationArguments[ONGOING_CALL_CONVERSATION_ID_ARG] as ConversationId)
                false
            }

            shouldGoToConversation() -> {
                openConversation(navigationArguments[OPEN_CONVERSATION_ID_ARG] as ConversationId)
                false
            }

            shouldGoToOtherProfile() -> {
                openOtherUserProfile(navigationArguments[OPEN_OTHER_USER_PROFILE_ARG] as QualifiedID)
                false
            }
            isServerConfigOnPremises() -> false

            intent == null -> false
            else -> true
        }
    }

    fun dismissCustomBackendDialog() {
        globalAppState = globalAppState.copy(customBackendDialog = CustomBEDeeplinkDialogState(shouldShowDialog = false))
    }

    fun customBackendDialogProceedButtonClicked(serverLinks: ServerConfig.Links) {
        viewModelScope.launch {
            dismissCustomBackendDialog()
            authServerConfigProvider.updateAuthServer(serverLinks)
            if (checkNumberOfSessions() == BuildConfig.MAX_ACCOUNTS) {
                globalAppState = globalAppState.copy(maxAccountDialog = true)
            } else {
                navigateTo(NavigationCommand(NavigationItem.Welcome.getRouteWithArgs()))
            }
        }
    }

    private suspend fun checkNumberOfSessions(): Int {
        getSessions().let {
            return when (it) {
                is GetAllSessionsResult.Success -> {
                    it.sessions.filterIsInstance<AccountInfo.Valid>().size
                }
                is GetAllSessionsResult.Failure.Generic -> 0
                GetAllSessionsResult.Failure.NoSessionFound -> 0
            }
        }
    }

    private fun openIncomingCall(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))))
    }

    private fun openOngoingCall(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))))
    }

    private fun openConversation(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.Conversation.getRouteWithArgs(listOf(conversationId)), BackStackMode.UPDATE_EXISTED))
    }

    private fun openOtherUserProfile(userId: QualifiedID) {
        navigateTo(NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(userId)), BackStackMode.UPDATE_EXISTED))
    }

    private fun isLaunchedFromHistory(intent: Intent?) =
        intent?.flags != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch {
            navigationManager.navigate(command)
        }
    }

    private suspend fun loadServerConfig(url: String): ServerConfig.Links? = when (val result = getServerConfigUseCase(url)) {
        is GetServerConfigResult.Success -> result.serverConfigLinks
        // TODO: show error message on failure
        is GetServerConfigResult.Failure.Generic -> {
            appLogger.e("something went wrong during handling the custom server deep link: ${result.genericFailure}")
            null
        }
    }

    private fun isServerConfigOnPremises(): Boolean =
        (navigationArguments[SERVER_CONFIG_ARG] as? ServerConfig.Links) != ServerConfig.DEFAULT

    private fun shouldGoToLogin(): Boolean =
        navigationArguments[SSO_DEEPLINK_ARG] != null

    private fun shouldGoToIncomingCall(): Boolean =
        (navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToConversation(): Boolean =
        (navigationArguments[OPEN_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToOngoingCall(): Boolean =
        (navigationArguments[ONGOING_CALL_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToOtherProfile(): Boolean = (navigationArguments[OPEN_OTHER_USER_PROFILE_ARG] as? QualifiedID) != null

    // TODO: the usage of currentSessionFlow is a temporary solution, it should be replaced with a proper solution
    private fun shouldGoToWelcome(): Boolean = runBlocking {
        currentSessionFlow().first().let {
            when (it) {
                is CurrentSessionResult.Failure.Generic -> true
                CurrentSessionResult.Failure.SessionNotFound -> true
                is CurrentSessionResult.Success -> false
            }
        }
    }

    private fun shouldGoToMigration(): Boolean = runBlocking {
        false // TODO implement later
    }

    fun openProfile() {
        dismissMaxAccountDialog()
        navigateTo(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
    }

    fun dismissMaxAccountDialog() {
        globalAppState = globalAppState.copy(maxAccountDialog = false)
    }

    companion object {
        private const val SERVER_CONFIG_ARG = "server_config"
        private const val SSO_DEEPLINK_ARG = "sso_deeplink"
        private const val INCOMING_CALL_CONVERSATION_ID_ARG = "incoming_call_conversation_id"
        private const val ONGOING_CALL_CONVERSATION_ID_ARG = "ongoing_call_conversation_id"
        private const val OPEN_CONVERSATION_ID_ARG = "open_conversation_id"
        private const val OPEN_OTHER_USER_PROFILE_ARG = "open_other_user_id"
    }
}

sealed class CurrentSessionErrorState {
    object RemovedClient : CurrentSessionErrorState()
    object DeletedAccount : CurrentSessionErrorState()
    object SessionExpired : CurrentSessionErrorState()
}

data class GlobalAppState(
    val customBackendDialog: CustomBEDeeplinkDialogState = CustomBEDeeplinkDialogState(),
    val maxAccountDialog: Boolean = false,
    val blockUserUI: CurrentSessionErrorState? = null
)
