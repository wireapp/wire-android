package com.wire.android.ui.initialsync

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitialSyncViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount private val userId: UserId,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    fun waitUntilSyncIsCompleted() {
        viewModelScope.launch(dispatchers.io()) {
            observeSyncState().collect { syncState ->
                if (syncState is SyncState.Live) {
                    userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
                    navigateToConvScreen()
                }
            }
        }
    }

    @VisibleForTesting
    fun navigateToConvScreen() = viewModelScope.launch {
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
    }
}
