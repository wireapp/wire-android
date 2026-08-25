/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

@file:Suppress("StringTemplate")

package com.wire.android.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wire.android.appLogger
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.navigation.routes.media.AuthenticatedImportMediaRoute
import com.wire.android.navigation.routes.media.LoggedOutImportMediaRoute
import com.wire.android.ui.home.conversations.ConversationRoute
import com.wire.android.ui.settings.devices.SelfDevicesRoute
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.android.ui.userprofile.toQualifiedId
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.WireRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicInteger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
class CurrentScreenManager @Inject constructor(
    screenStateObserver: ScreenStateObserver
) : DefaultLifecycleObserver {

    private var stopAnalyticsView: (String) -> Unit = AnonymousAnalyticsManagerImpl::stopView
    private var recordAnalyticsView: (String) -> Unit = AnonymousAnalyticsManagerImpl::recordView

    internal constructor(
        screenStateObserver: ScreenStateObserver,
        anonymousAnalyticsManager: AnonymousAnalyticsManager,
    ) : this(screenStateObserver) {
        stopAnalyticsView = anonymousAnalyticsManager::stopView
        recordAnalyticsView = anonymousAnalyticsManager::recordView
    }

    private val currentScreenState = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther())

    /**
     * An integer that counts up when a screen appears, and counts down when
     * the screen goes away.
     * Better than a simple boolean in cases where an activity is re-started,
     * which may result the new instance being shown BEFORE the old instance being hidden.
     */
    private val visibilityCount = AtomicInteger(0)
    private val isApplicationVisibleFlow = MutableStateFlow(false)
    private val isAppVisibleFlow = screenStateObserver.screenStateFlow.combine(
        isApplicationVisibleFlow
    ) { isScreenOn, isOnForeground ->
        isOnForeground && isScreenOn
    }

    suspend fun observeCurrentScreen(scope: CoroutineScope): StateFlow<CurrentScreen> = isAppVisibleFlow
        .flatMapLatest { isAppVisible ->
            if (isAppVisible) {
                currentScreenState
            } else {
                flowOf(CurrentScreen.InBackground)
            }
        }
        .distinctUntilChanged()
        .stateIn(scope)

    /**
     * Informs if the UI is visible at the moment.
     * Visibility doesn't necessarily mean being on the foreground. For example,
     * if the device screen is split into multiple activities, and the app is currently not being focused,
     * the app is considered to be on the background, but **still visible and working** fine.
     */
    fun isAppVisibleFlow(): StateFlow<Boolean> = isApplicationVisibleFlow

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appLogger.i("${TAG}: app onStart called")
        visibilityCount.getAndUpdate { currentValue ->
            val newValue = maxOf(0, currentValue + 1)
            isApplicationVisibleFlow.value = newValue > 0
            newValue
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appLogger.i("${TAG}: app onStop called")
        visibilityCount.getAndUpdate { currentValue ->
            val newValue = maxOf(0, currentValue - 1)
            isApplicationVisibleFlow.value = newValue > 0
            newValue
        }
    }

    fun onRouteChanged(route: WireRoute) {
        changeCurrentScreen(CurrentScreen.fromRoute(route, isApplicationVisibleFlow.value))
    }

    private fun changeCurrentScreen(newScreen: CurrentScreen) {
        stopAnalyticsView(currentScreenName())
        currentScreenState.value = newScreen
        recordAnalyticsView(currentScreenName())
    }

    private fun currentScreenName() = currentScreenState.value.let { currentScreen ->
        when (currentScreen) {
            is CurrentScreen.Home,
            is CurrentScreen.Conversation,
            is CurrentScreen.OtherUserProfile,
            is CurrentScreen.ImportMedia,
            is CurrentScreen.DeviceManager -> return@let currentScreen.toScreenName()

            is CurrentScreen.AuthRelated -> return@let currentScreen.route ?: currentScreen.toString()
            else -> return@let (currentScreen as? CurrentScreen.SomeOther)?.route ?: currentScreen.toString()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        appLogger.i("$TAG app onCreate called")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLogger.i("$TAG app onResume called")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appLogger.i("$TAG app onPause called")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        appLogger.i("$TAG app onDestroy called")
    }

    companion object {
        private const val TAG = "CurrentScreenManager"
    }
}

sealed class CurrentScreen {

    // Home Screen is being displayed
    data object Home : CurrentScreen() {
        override fun toScreenName() = "HomeScreen"
    }

    // Some Conversation is opened
    data class Conversation(val id: ConversationId) : CurrentScreen() {
        override fun toString(): String = "Conversation(${id.value.obfuscateId()})"
        override fun toScreenName() = "ConversationScreen"
    }

    // Another User Profile Screen is opened
    data class OtherUserProfile(val userId: UserId, val groupConversationId: ConversationId?) : CurrentScreen() {
        override fun toString(): String = "OtherUserProfile(${userId.value.obfuscateId()}, ${groupConversationId?.value?.obfuscateId()})"
        override fun toScreenName() = "OtherUserProfileScreen"
    }

    // Import media screen is opened
    data object ImportMedia : CurrentScreen() {
        override fun toScreenName() = "ImportMediaScreen"
    }

    // SelfDevices screen is opened
    data object DeviceManager : CurrentScreen() {
        override fun toScreenName() = "DeviceManagerScreen"
    }

    // Auth related screen is opened
    data class AuthRelated(val route: String?) : CurrentScreen()

    // Some other screen is opened, kinda "do nothing screen"
    data class SomeOther(val route: String? = null) : CurrentScreen()

    // App is in background (screen is turned off, or covered by another app), non of the screens is visible
    data object InBackground : CurrentScreen()

    open fun toScreenName(): String = "UnknownScreen"

    companion object {
        fun fromRoute(route: WireRoute, isAppVisible: Boolean): CurrentScreen {
            if (!isAppVisible) {
                return InBackground
            }
            return when (route) {
                is HomeRoute -> Home
                is ConversationRoute -> Conversation(
                    ConversationId(route.conversationId.value, route.conversationId.domain)
                )
                is AuthenticatedImportMediaRoute,
                is LoggedOutImportMediaRoute -> ImportMedia
                is OtherUserProfileRoute -> OtherUserProfile(
                    userId = route.targetUserId.toQualifiedId(),
                    groupConversationId = route.groupConversationId?.toQualifiedId(),
                )
                is SelfDevicesRoute -> DeviceManager
                is AuthenticationScreenRoute -> AuthRelated(route.routeId)
                else -> SomeOther(route.routeId)
            }
        }
    }
}
