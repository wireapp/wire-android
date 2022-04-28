package com.wire.android.ui

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.GetNotificationsUseCaseProvider
import com.wire.android.navigation.NavigationItem
import com.wire.android.notification.MessageNotificationManager
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
    private val getServerConfigUserCase: GetServerConfigUseCase,
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

    fun startNavigationRoute() = when {
        serverConfig.apiBaseUrl != ServerConfig.DEFAULT.apiBaseUrl -> NavigationItem.Login.getRouteWithArgs()
        isUserLoggedIn -> NavigationItem.Home.getRouteWithArgs()
        else -> NavigationItem.Welcome.getRouteWithArgs()
    }

    private fun loadServerConfig(url: String) = runBlocking {
        return@runBlocking when (val result = getServerConfigUserCase(url)) {
            is GetServerConfigResult.Success -> result.serverConfig
            else -> ServerConfig.DEFAULT
        }
    }

    fun handleDeepLink(intent: Intent) {
        intent.data?.getQueryParameter(SERVER_CONFIG_DEEPLINK)?.let {
            serverConfig = loadServerConfig(it)
        }
    }

    init {
        viewModelScope.launch {
            listenForNotificationsIfPossible()
        }
    }

    private suspend fun listenForNotificationsIfPossible() {
        withContext(dispatchers.io()) {
            //TODO this intervalFlow is a temporary solution to have updated UserId,
            // waiting for refactoring in kalium
            intervalFlow(60_000)
                .map {
                    when (val result = currentSessionUseCase()) {
                        is CurrentSessionResult.Success -> result.authSession.userId
                        else -> null
                    }
                }
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId != null) {
                        getNotificationProvider.create(userId).getNotifications()
                    } else {
                        flowOf(listOf())
                    }
                        .scan((listOf<LocalNotificationConversation>() to listOf<LocalNotificationConversation>()))
                        { old, newList -> old.second to newList }
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
        const val SERVER_CONFIG_DEEPLINK = "config"
    }
}
