/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details.updateappsaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.upgradetoapps.UpgradeToGetAppsBanner
import com.wire.android.ui.home.conversations.details.options.DisableConfirmationDialog
import com.wire.android.ui.home.conversations.details.options.GroupOptionWithSwitch
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.launch

@WireDestination(
    navArgsDelegate = UpdateAppsAccessNavArgs::class,
    style = DestinationStyle.Runtime::class,
)
@Composable
fun UpdateAppsAccessScreen(
    navigator: Navigator,
    updateAppsAccessViewModel: UpdateAppsAccessViewModel = hiltViewModel()
) {
    UpdateAppsAccessContent(
        onNavigateBack = navigator::navigateBack,
        onChangeAppAccess = updateAppsAccessViewModel::onAppsAccessUpdate,
        onDisableAppsConfirm = updateAppsAccessViewModel::onServiceDialogConfirm,
        onDisableAppsDismiss = updateAppsAccessViewModel::onAppsDialogDismiss,
        state = updateAppsAccessViewModel.updateAppsAccessState,
    )
}

@Composable
private fun UpdateAppsAccessContent(
    onNavigateBack: () -> Unit,
    onChangeAppAccess: (Boolean) -> Unit,
    onDisableAppsConfirm: () -> Unit,
    onDisableAppsDismiss: () -> Unit,
    state: UpdateAppsAccessState,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    WireScaffold(
        modifier = modifier,
        topBar = {
            val title = stringResource(id = R.string.apps_edit_apps_option_title)
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                navigationIconType = NavigationIconType.Back(R.string.content_description_edit_apps_option_back_btn),
                onNavigationPressed = onNavigateBack,
                title = title
            )
        }
    ) { internalPadding ->
        Column {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.wireColorScheme.surface)
                    .padding(internalPadding)
                    .weight(1f)
                    .fillMaxSize()
            ) {
                item {
                    with(state) {
                        GroupOptionWithSwitch(
                            switchClickable = isUpdatingAppAccessAllowed,
                            switchVisible = true,
                            switchState = isAppAccessAllowed,
                            onClick = onChangeAppAccess,
                            isLoading = isLoadingAppsOption,
                            title = R.string.conversation_options_services_label,
                            subTitle = R.string.conversation_options_services_description
                        )
                    }
                }
                if (!state.isUpdatingAppAccessAllowed) {
                    item {
                        UpgradeToGetAppsBanner()
                    }
                }
            }
        }
    }

    if (state.shouldShowDisableAppsConfirmationDialog) {
        DisableServicesConfirmationDialog(
            onConfirm = onDisableAppsConfirm,
            onDialogDismiss = onDisableAppsDismiss
        )
    }

    if (state.hasErrorOnUpdateAppAccess) {
        val errorMessage = stringResource(R.string.label_general_error)
        LaunchedEffect(true) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }
}

@Composable
private fun DisableServicesConfirmationDialog(onConfirm: () -> Unit, onDialogDismiss: () -> Unit) {
    DisableConfirmationDialog(
        title = R.string.disable_services_dialog_title,
        text = R.string.disable_services_dialog_text,
        onDismiss = onDialogDismiss,
        onConfirm = onConfirm
    )
}

@PreviewMultipleThemes
@Composable
fun UpdateAppsAccessEnabledPreview() = WireTheme {
    UpdateAppsAccessContent(
        onNavigateBack = {},
        onChangeAppAccess = {},
        onDisableAppsConfirm = {},
        onDisableAppsDismiss = {},
        state = UpdateAppsAccessState(
            isAppAccessAllowed = true,
            isUpdatingAppAccessAllowed = true,
            isLoadingAppsOption = false,
            shouldShowDisableAppsConfirmationDialog = false
        ),
    )
}

@PreviewMultipleThemes
@Composable
fun UpdateAppsAccessDisabledPreview() = WireTheme {
    UpdateAppsAccessContent(
        onNavigateBack = {},
        onChangeAppAccess = {},
        onDisableAppsConfirm = {},
        onDisableAppsDismiss = {},
        state = UpdateAppsAccessState(
            isAppAccessAllowed = true,
            isUpdatingAppAccessAllowed = false,
            isLoadingAppsOption = false,
            shouldShowDisableAppsConfirmationDialog = false
        ),
    )
}
