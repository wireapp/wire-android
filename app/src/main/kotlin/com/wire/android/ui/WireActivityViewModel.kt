package com.wire.android.ui

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.GetNotificationsUseCaseProvider
import com.wire.android.navigation.NavigationItem
import com.wire.android.notification.MessageNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extension.intervalFlow
import com.wire.kalium.logic.configuration.GetServerConfigResult
import com.wire.kalium.logic.configuration.GetServerConfigUseCase
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalMaterial3Api
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val currentSessionUseCase: CurrentSessionUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val getNotificationProvider: GetNotificationsUseCaseProvider.Factory,
    private val notificationManager: MessageNotificationManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val currentSession: AuthSession? = runBlocking {
        return@runBlocking when (val result = currentSessionUseCase()) {
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
            listenForNotificationsIfPossible()
        }
    }

    private suspend fun listenForNotificationsIfPossible() {
        withContext(dispatchers.io()) {
            // checking CurrentSession every minute, to subscribe/unsubscribe from the notifications
            // according ot UserId changes
            // TODO this intervalFlow is a temporary solution to have updated UserId,
            // waiting for refactoring in kalium
            intervalFlow(CHECK_USER_ID_FREQUENCY_MS)
                .map {
                    when (val result = currentSessionUseCase()) {
                        is CurrentSessionResult.Success -> result.authSession.userId
                        else -> null
                    }
                }
                // do nothing if UserId wasn't changed
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId != null) {
                        getNotificationProvider.create(userId).getNotifications()
                    } else {
                        // if userId == null means there is no current user (logged out e.x.)
                        // so we need to unsubscribe from the notification changes (it's done by `flatMapLatest`)
                        // and remove the notifications that were displayed previously
                        // (empty list in here makes all the pre. notifications be removed)
                        flowOf(listOf())
                    }
                        // we need to remember prev. displayed Notifications,
                        // so we can remove notifications that were displayed previously but are not in the new list
                        .scan((listOf<LocalNotificationConversation>() to listOf<LocalNotificationConversation>()))
                        { old, newList -> old.second to newList }
                        // combining all the data that is necessary for Notifications into small data class,
                        // just to make it more readable than
                        // Triple<List<LocalNotificationConversation>, List<LocalNotificationConversation>, QualifiedID?>
                        .map { (oldNotifications, newNotifications) ->
                            NotificationsData(oldNotifications, newNotifications, userId)
                        }
                }
                .collect { (oldNotifications, newNotifications, userId) ->
                    notificationManager.handleNotification(oldNotifications, newNotifications, userId)
                }
        }
    }

    private data class NotificationsData(
        val oldNotifications: List<LocalNotificationConversation>,
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID?
    )

    companion object {
        private const val CHECK_USER_ID_FREQUENCY_MS = 60_000L
    }
}
