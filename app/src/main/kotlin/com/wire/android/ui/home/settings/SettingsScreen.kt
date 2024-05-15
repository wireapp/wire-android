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

package com.wire.android.ui.home.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.model.Clickable
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.SetLockCodeScreenDestination
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.extension.folderWithElements

@HomeNavGraph
@Destination
@Composable
fun SettingsScreen(
    homeStateHolder: HomeStateHolder,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val lazyListState: LazyListState = rememberLazyListState()
    val turnAppLockOffDialogState = rememberVisibilityState<Unit>()
    val onAppLockSwitchClicked: (Boolean) -> Unit = remember {
        { isChecked ->
            if (isChecked) homeStateHolder.navigator.navigate(NavigationCommand(SetLockCodeScreenDestination, BackStackMode.NONE))
            else turnAppLockOffDialogState.show(Unit)
        }
    }

    val context = LocalContext.current
    SettingsScreenContent(
        lazyListState = lazyListState,
        settingsState = viewModel.state,
        onItemClicked = remember {
            {
                it.direction.handleNavigation(
                    context = context,
                    handleOtherDirection = { homeStateHolder.navigator.navigate(NavigationCommand(it)) }
                )
            }
        },
        onAppLockSwitchChanged = onAppLockSwitchClicked
    )
    TurnAppLockOffDialog(dialogState = turnAppLockOffDialogState, turnOff = viewModel::disableAppLock)
}

@Composable
fun SettingsScreenContent(
    lazyListState: LazyListState = rememberLazyListState(),
    settingsState: SettingsState,
    onItemClicked: (SettingsItem.DirectionItem) -> Unit,
    onAppLockSwitchChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val featureVisibilityFlags = LocalFeatureVisibilityFlags.current

    with(featureVisibilityFlags) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            folderWithElements(
                header = context.getString(R.string.settings_account_settings_label),
                items = buildList {
                    add(SettingsItem.YourAccount)
                    add(SettingsItem.Appearance)
                    add(SettingsItem.PrivacySettings)
                    add(SettingsItem.ManageDevices)
                    if (BackUpSettings) {
                        add(SettingsItem.BackupAndRestore)
                    }
                },
                onItemClicked = onItemClicked
            )

            folderWithElements(
                header = context.getString(R.string.app_settings_screen_title),
                items = buildList {
                    if (AppSettings) {
                        add(SettingsItem.AppSettings)
                    }
                    add(SettingsItem.NetworkSettings)

                    appLogger.d("AppLockConfig " +
                            "isAppLockEditable: ${settingsState.isAppLockEditable} isAppLockEnabled: ${settingsState.isAppLockEnabled}")
                    add(SettingsItem.AppLock(
                        when (settingsState.isAppLockEditable) {
                            true -> {
                                SwitchState.Enabled(
                                    value = settingsState.isAppLockEnabled,
                                    isOnOffVisible = true,
                                    onCheckedChange = onAppLockSwitchChanged
                                )
                            }

                            false -> {
                                SwitchState.TextOnly(
                                    value = settingsState.isAppLockEnabled,
                                )
                            }
                        }
                    ))
                },
                onItemClicked = onItemClicked
            )

            folderWithElements(
                header = context.getString(R.string.settings_other_group_title),
                items = buildList {
                    add(SettingsItem.Support)
                    if (BuildConfig.DEBUG_SCREEN_ENABLED) {
                        add(SettingsItem.DebugSettings)
                    }
                    add(SettingsItem.GiveFeedback)
                    add(SettingsItem.ReportBug)
                    add(SettingsItem.AboutApp)
                },

                onItemClicked = onItemClicked
            )
        }
    }
}

private fun LazyListScope.folderWithElements(
    header: String,
    items: List<SettingsItem>,
    onItemClicked: (SettingsItem.DirectionItem) -> Unit
) {
    folderWithElements(
        header = header.uppercase(),
        items = items.associateBy { it.id }
    ) { settingsItem ->
        SettingsItem(
            text = settingsItem.title.asString(),
            switchState = (settingsItem as? SettingsItem.SwitchItem)?.switchState ?: SwitchState.None,
            onRowPressed = remember {
                Clickable(enabled = settingsItem is SettingsItem.DirectionItem) {
                    (settingsItem as? SettingsItem.DirectionItem)?.let(onItemClicked)
                }
            },
            trailingIcon = if (settingsItem is SettingsItem.DirectionItem) R.drawable.ic_arrow_right else null,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewSettingsScreen() {
    SettingsScreenContent(rememberLazyListState(), SettingsState(), {}, {})
}
