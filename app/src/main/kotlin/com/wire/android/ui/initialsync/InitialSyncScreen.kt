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
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.ramcosta.composedestinations.generated.app.destinations.HomeScreenDestination
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.getBaseRoute
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.common.SettingUpWireScreenContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@WireRootDestination
@Composable
fun InitialSyncScreen(
    navigator: Navigator,
    viewModel: InitialSyncViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current
    val syncCompletionState = viewModel.syncCompletionState

    SettingUpWireScreenContent()

    LaunchedEffect(syncCompletionState) {
        syncCompletionState ?: return@LaunchedEffect

        if (syncCompletionState.shouldMoveToBackground) {
            activity.lifecycleScope.launch {
                navigator.navController.currentBackStackEntryFlow
                    .map { it.destination.route?.getBaseRoute() }
                    .first { it == HomeScreenDestination.baseRoute }
                activity.moveTaskToBack(false)
            }
        }

        navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
    }
}
