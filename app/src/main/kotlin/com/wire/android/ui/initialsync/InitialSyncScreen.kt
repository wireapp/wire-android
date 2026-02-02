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

package com.wire.android.ui.initialsync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.WireActivityViewModel
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.destinations.HomeScreenDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@WireDestination
@Composable
fun InitialSyncScreen(
    navigator: Navigator,
    viewModel: InitialSyncViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current
    val wireActivityViewModel: WireActivityViewModel = viewModel(viewModelStoreOwner = activity)
    val scope = rememberCoroutineScope()

    SettingUpWireScreenContent()

    if (viewModel.isSyncCompleted) {
        navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))

        // If started with SSO intent, move app to background after sync completes
        if (wireActivityViewModel.globalAppState.startedWithSSOIntent) {
            appLogger.i("InitialSyncScreen: SSO intent detected and sync completed, moving app to background")
            wireActivityViewModel.setSSOIntentFlag(false)
            scope.launch {
                delay(100) // Small delay to let navigation complete
                appLogger.i("InitialSyncScreen: Calling moveTaskToBack(false)")
                val result = activity.moveTaskToBack(false)
                appLogger.i("InitialSyncScreen: moveTaskToBack result: $result")
            }
        }
    }
}
