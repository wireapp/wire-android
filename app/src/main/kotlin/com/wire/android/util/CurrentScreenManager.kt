package com.wire.android.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import cafe.adriel.voyager.core.screen.Screen
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
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
class CurrentScreenManager @Inject constructor() : DefaultLifecycleObserver {

    private val currentScreenState = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther)
    private val isAppVisibleFlow = MutableStateFlow(true)
    private val wasAppEverVisibleFlow = MutableStateFlow(false)

    suspend fun observeCurrentScreen(scope: CoroutineScope): StateFlow<CurrentScreen> = isAppVisibleFlow
        .flatMapLatest { isAppVisible ->
            if (isAppVisible) currentScreenState
            else flowOf(CurrentScreen.InBackground)
        }
        .stateIn(scope)

    /**
     * Informs if the UI was visible at least once since the app started
     */
    fun appWasVisibleAtLeastOnceFlow(): StateFlow<Boolean> = wasAppEverVisibleFlow

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isAppVisibleFlow.value = true
        wasAppEverVisibleFlow.value = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isAppVisibleFlow.value = false
    }

    fun onDestinationChanged(destination: Screen) {
        currentScreenState.value = CurrentScreen.fromNavigationItem(destination as? VoyagerNavigationItem)
    }
}

sealed class CurrentScreen {

    // Some Conversation is opened
    data class Conversation(val id: ConversationId) : CurrentScreen()
    // Another User Profile Screen is opened
    data class OtherUserProfile(val id: QualifiedID) : CurrentScreen()
    // Some other screen is opened, kinda "do nothing screen"
    object SomeOther : CurrentScreen()
    // App is in background (screen is turned off, or covered by another app), non of the screens is visible
    object InBackground : CurrentScreen()

    companion object {

        fun fromNavigationItem(currentItem: VoyagerNavigationItem?): CurrentScreen =
            when (currentItem) {
                is VoyagerNavigationItem.Conversation -> Conversation(currentItem.conversationId.qualifiedId)
                is VoyagerNavigationItem.OtherUserProfile -> OtherUserProfile(currentItem.userId.qualifiedId)
                else -> SomeOther
            }
    }
}
