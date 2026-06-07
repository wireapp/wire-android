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

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.ramcosta.composedestinations.generated.app.destinations.HomeScreenDestination
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.getBaseRoute
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.initialSyncViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@WireRootDestination
@Composable
fun InitialSyncScreen(
    navigator: Navigator,
    viewModel: InitialSyncViewModel = initialSyncViewModel()
) {
    val activity = LocalActivity.current
    val syncCompletionState = viewModel.syncCompletionState

    SettingUpWireScreenContent(
        secondLineMessage = if (viewModel.isRestoringBackup) {
            stringResource(R.string.backup_dialog_restoring_backup_title)
        } else {
            null
        }
    )

    if (viewModel.showBackupRootKeyUnavailableDialog) {
        WireDialog(
            title = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_title),
            text = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_message),
            onDismiss = viewModel::onBackupRootKeyDialogCancel,
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = viewModel::onBackupRootKeyDialogCancel,
                type = WireDialogButtonType.Secondary,
            ),
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_try_again),
                onClick = viewModel::onBackupRootKeyDialogTryAgain,
                type = WireDialogButtonType.Primary,
            ),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.restoreErrorToast.collect { messageResId ->
            Toast.makeText(activity, activity.getString(messageResId), Toast.LENGTH_SHORT).show()
        }
    }

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
