package com.wire.android.ui

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.GetNotificationsUseCaseProvider
import com.wire.android.navigation.NavigationItem
import com.wire.android.notification.MessageNotificationManager
import com.wire.android.notification.NotificationConversation
import com.wire.android.notification.NotificationData
import com.wire.kalium.logic.configuration.GetServerConfigResult
import com.wire.kalium.logic.configuration.GetServerConfigUseCase
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val currentSessionUseCase: CurrentSessionUseCase,
    private val getServerConfigUserCase: GetServerConfigUseCase,
    private val getNotificationProvider: GetNotificationsUseCaseProvider.Factory,
    private val notificationManager: MessageNotificationManager
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
            val currentSessionResult = currentSessionUseCase()
            if (currentSessionResult is CurrentSessionResult.Success) {
                listenForNotifications(currentSessionResult.authSession.userId)
            }
        }
    }

    private suspend fun listenForNotifications(userId: UserId) {
        getNotificationProvider.create(userId)
            .getNotifications()
            .map { list -> list.mapNotNull { either -> either.fold({ null }) { it } } }
            .filter { it.isNotEmpty() }
            .collect {
                notificationManager.showNotification(NotificationData(it.map { dbConversation ->
                    NotificationConversation.fromDbData(dbConversation)
                }))
            }
    }

    companion object {
        const val SERVER_CONFIG_DEEPLINK = "config"
    }
}
