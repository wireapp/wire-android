/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

@file:Suppress("StringTemplate")

package com.wire.android.util

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.getCurrentNavigationItem
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.id.toQualifiedID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CurrentScreenManager @Inject constructor(
    @ApplicationContext val context: Context,
    screenStateObserver: ScreenStateObserver
) : DefaultLifecycleObserver,
    NavController.OnDestinationChangedListener {

    private val currentScreenState = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther)
    private val isOnForegroundFlow = MutableStateFlow(false)
    private val isAppVisibleFlow = screenStateObserver.screenStateFlow.combine(isOnForegroundFlow) { isScreenOn, isOnForeground ->
        isOnForeground && isScreenOn
    }

    suspend fun observeCurrentScreen(scope: CoroutineScope): StateFlow<CurrentScreen> = isAppVisibleFlow
        .flatMapLatest { isAppVisible ->
            if (isAppVisible) currentScreenState
            else flowOf(CurrentScreen.InBackground)
        }
        .distinctUntilChanged()
        .stateIn(scope)

    /**
     * Informs if the UI was visible at least once since the app started
     */
    fun isAppOnForegroundFlow(): StateFlow<Boolean> = isOnForegroundFlow

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLogger.i("${TAG}: onResume called")
        isOnForegroundFlow.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appLogger.i("${TAG}: onStop called")
        isOnForegroundFlow.value = false
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val currentItem = controller.getCurrentNavigationItem()
        currentScreenState.value = CurrentScreen.fromNavigationItem(currentItem, arguments, isOnForegroundFlow.value)
    }

    companion object {
        private const val TAG = "CurrentScreenManager"
    }
}

sealed class CurrentScreen {

    // Home Screen is being displayed
    object Home : CurrentScreen()

    // Some Conversation is opened
    data class Conversation(val id: ConversationId) : CurrentScreen()

    // Another User Profile Screen is opened
    data class OtherUserProfile(val id: ConversationId) : CurrentScreen()

    // Ongoing call screen is opened
    data class OngoingCallScreen(val id: QualifiedID) : CurrentScreen()

    // Incoming call screen is opened
    data class IncomingCallScreen(val id: QualifiedID) : CurrentScreen()

    // Some other screen is opened, kinda "do nothing screen"
    object SomeOther : CurrentScreen()

    // App is in background (screen is turned off, or covered by another app), non of the screens is visible
    object InBackground : CurrentScreen()

    companion object {
        val qualifiedIdMapper = QualifiedIdMapperImpl(null)

        @Suppress("ComplexMethod")
        fun fromNavigationItem(currentItem: NavigationItem?, arguments: Bundle?, isAppVisible: Boolean): CurrentScreen {
            if (!isAppVisible) {
                return InBackground
            }
            return when (currentItem) {
                NavigationItem.Home -> Home
                NavigationItem.Conversation -> {
                    arguments?.getString(EXTRA_CONVERSATION_ID)
                        ?.toQualifiedID(qualifiedIdMapper)
                        ?.let { Conversation(it) }
                        ?: SomeOther
                }
                NavigationItem.OtherUserProfile -> {
                    arguments?.getString(EXTRA_CONVERSATION_ID)
                        ?.toQualifiedID(qualifiedIdMapper)
                        ?.let { OtherUserProfile(it) }
                        ?: SomeOther
                }
                NavigationItem.OngoingCall -> {
                    arguments?.getString(EXTRA_CONVERSATION_ID)
                        ?.toQualifiedID(qualifiedIdMapper)
                        ?.let { OngoingCallScreen(it) }
                        ?: SomeOther
                }
                NavigationItem.IncomingCall -> {
                    arguments?.getString(EXTRA_CONVERSATION_ID)
                        ?.toQualifiedID(qualifiedIdMapper)
                        ?.let { IncomingCallScreen(it) }
                        ?: SomeOther
                }
                else -> SomeOther
            }
        }
    }
}
