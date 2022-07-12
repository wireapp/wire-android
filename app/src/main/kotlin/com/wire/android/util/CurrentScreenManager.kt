package com.wire.android.util

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.getCurrentNavigationItem
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Singleton
class CurrentScreenManager @Inject constructor() : DefaultLifecycleObserver, NavController.OnDestinationChangedListener {

    private val currentScreenState = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther)
    private val isAppVisibleFlow = MutableStateFlow(true)

    suspend fun observeCurrentScreen(scope: CoroutineScope): StateFlow<CurrentScreen> = isAppVisibleFlow
        .flatMapLatest { isAppVisible ->
            if (isAppVisible) currentScreenState
            else flowOf(CurrentScreen.InBackground)
        }
        .stateIn(scope)

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isAppVisibleFlow.value = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isAppVisibleFlow.value = false
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val currentItem = controller.getCurrentNavigationItem()
        currentScreenState.value = CurrentScreen.fromNavigationItem(currentItem, arguments)
    }
}

sealed class CurrentScreen {

    // Some Conversation is opened
    data class Conversation(val id: ConversationId) : CurrentScreen()
    // Some other screen is opened, kinda "do nothing screen"
    object SomeOther : CurrentScreen()
    // App is in background (screen is turned off, or covered by another app), non of the screens is visible
    object InBackground : CurrentScreen()

    companion object {
        fun fromNavigationItem(currentItem: NavigationItem?, arguments: Bundle?): CurrentScreen =
            when (currentItem) {
                NavigationItem.Conversation -> {
                    arguments?.getString(EXTRA_CONVERSATION_ID)
                        ?.parseIntoQualifiedID()
                        ?.let { Conversation(it) }
                        ?: SomeOther
                }
                else -> SomeOther
            }
    }
}
