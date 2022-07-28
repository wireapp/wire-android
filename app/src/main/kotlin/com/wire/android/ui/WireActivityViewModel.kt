package com.wire.android.ui

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.navigation.nav
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
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
) : ViewModel() {

    var deepLinkDestination: DeepLinkDestination by mutableStateOf(DeepLinkDestination.None)
        private set

    private val observeUserId = currentSessionFlow()
        .map { result ->
            if (result is CurrentSessionResult.Success) result.authSession.tokens.userId
            else null
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    init {
        viewModelScope.launch(dispatchers.io()) {
            notificationManager.observeNotificationsAndCalls(observeUserId, viewModelScope) { openIncomingCall(it.conversationId) }
        }
    }

    fun startVoyagerNavigationScreen(): List<VoyagerNavigationItem> =
        when (val destination = deepLinkDestination) {
            DeepLinkDestination.Welcome -> listOf(VoyagerNavigationItem.Welcome)
            DeepLinkDestination.None -> listOf(VoyagerNavigationItem.Home)
            is DeepLinkDestination.Login ->
                listOf(
                    VoyagerNavigationItem.Welcome,
                    VoyagerNavigationItem.Login(destination.ssoLogin)
                )
            is DeepLinkDestination.Conversation -> listOf(
                VoyagerNavigationItem.Home,
                VoyagerNavigationItem.Conversation(destination.conversationId.nav())
            )
            is DeepLinkDestination.IncomingCall -> listOf(
                VoyagerNavigationItem.Home,
                VoyagerNavigationItem.IncomingCall(destination.conversationId.nav())
            )
            is DeepLinkDestination.OtherUserProfile -> listOf(
                VoyagerNavigationItem.Home,
                VoyagerNavigationItem.OtherUserProfile(destination.userId.nav())
            )
        }

    fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { deepLink ->
            when (val result = deepLinkProcessor(deepLink)) {
                is DeepLinkResult.CustomServerConfig ->
                    loadServerConfig(result.url)?.let { serverLinks ->
                        authServerConfigProvider.updateAuthServer(serverLinks)
                        deepLinkDestination = DeepLinkDestination.Login(null)
                    }
                is DeepLinkResult.SSOLogin ->
                    deepLinkDestination = DeepLinkDestination.Login(result)
                is DeepLinkResult.IncomingCall -> {
                    if (isLaunchedFromHistory(intent)) {
                        //We don't need to handle deepLink, if activity was launched from history.
                        //For example: user opened app by deepLink, then closed it by back button click,
                        //then open the app from the "Recent Apps"
                        appLogger.i("IncomingCall deepLink launched from the history")
                    } else {
                        deepLinkDestination = DeepLinkDestination.IncomingCall(result.conversationsId)
                    }
                }
                is DeepLinkResult.OpenConversation -> {
                    if (isLaunchedFromHistory(intent)) {
                        appLogger.i("OpenConversation deepLink launched from the history")
                    } else {
                        deepLinkDestination = DeepLinkDestination.Conversation(result.conversationsId)
                    }
                }
                is DeepLinkResult.OpenOtherUserProfile -> {
                    if (isLaunchedFromHistory(intent)) {
                        appLogger.i("OpenOtherUserProfile deepLink launched from the history")
                    } else {
                        deepLinkDestination = DeepLinkDestination.OtherUserProfile(result.userId)
                    }
                }
                DeepLinkResult.Unknown -> {
                    appLogger.e("unknown deeplink result $result")
                    deepLinkDestination = DeepLinkDestination.None
                }
            }
        } ?: run { deepLinkDestination = DeepLinkDestination.None }
    }

    fun handleDeepLinkOnNewIntent(intent: Intent?) {

        //removing arguments that could be there from prev deeplink handling
        deepLinkDestination = DeepLinkDestination.None

        handleDeepLink(intent)

        when (val destination = deepLinkDestination) {
            DeepLinkDestination.None -> {}
            DeepLinkDestination.Welcome -> openWelcome()
            is DeepLinkDestination.Login -> openLogin(destination.ssoLogin)
            is DeepLinkDestination.Conversation -> openConversation(destination.conversationId)
            is DeepLinkDestination.IncomingCall -> openIncomingCall(destination.conversationId)
            is DeepLinkDestination.OtherUserProfile -> openOtherUserProfile(destination.userId)
        }
    }

    private fun openWelcome() {
        navigateTo(NavigationCommand(VoyagerNavigationItem.Welcome, BackStackMode.CLEAR_WHOLE))
    }

    private fun openLogin(ssoLogin: DeepLinkResult.SSOLogin?) {
        navigateTo(
            NavigationCommand(
                listOf(VoyagerNavigationItem.Welcome, VoyagerNavigationItem.Login(ssoLogin)),
                BackStackMode.CLEAR_WHOLE
            )
        )
    }

    private fun openIncomingCall(conversationId: ConversationId) {
        navigateTo(NavigationCommand(VoyagerNavigationItem.IncomingCall(conversationId.nav())))
    }

    private fun openConversation(conversationId: ConversationId) {
        navigateTo(NavigationCommand(VoyagerNavigationItem.Conversation(conversationId.nav()), BackStackMode.UPDATE_EXISTED))
    }

    private fun openOtherUserProfile(userId: QualifiedID) {
        navigateTo(NavigationCommand(VoyagerNavigationItem.OtherUserProfile(userId.nav()), BackStackMode.UPDATE_EXISTED))
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

    sealed class DeepLinkDestination {
        object None : DeepLinkDestination()
        object Welcome : DeepLinkDestination()
        data class Login(val ssoLogin: DeepLinkResult.SSOLogin?) : DeepLinkDestination()
        data class IncomingCall(val conversationId: ConversationId) : DeepLinkDestination()
        data class Conversation(val conversationId: ConversationId) : DeepLinkDestination()
        data class OtherUserProfile(val userId: QualifiedID) : DeepLinkDestination()
    }
}
