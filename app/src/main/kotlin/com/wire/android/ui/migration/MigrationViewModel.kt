package com.wire.android.ui.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.workmanager.worker.MigrationWorker
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val getCurrentSession: CurrentSessionUseCase,
    private val workManager: WorkManager,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    fun init() {
        viewModelScope.launch(dispatchers.io()) {
            enqueueMigrationAndListenForStateChanges()
        }
    }

    private suspend fun enqueueMigrationAndListenForStateChanges() {
        MigrationWorker.enqueue(workManager).collect { state ->
            if (state == WorkInfo.State.SUCCEEDED)
                navigateAfterMigration()
        }
    }

    private suspend fun navigateAfterMigration() {
        when (getCurrentSession()) {
            is CurrentSessionResult.Success ->
                navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
            else ->
                navigationManager.navigate(NavigationCommand(NavigationItem.Welcome.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        }
    }
}
