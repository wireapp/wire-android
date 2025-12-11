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
package com.wire.android.feature.cells.ui.versioning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.ErrorScreen
import com.wire.android.feature.cells.ui.common.LoadingScreen
import com.wire.android.feature.cells.ui.versioning.restore.RestoreDialogState
import com.wire.android.feature.cells.ui.versioning.restore.RestoreNodeVersionConfirmationDialog
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.toUIText
import kotlinx.coroutines.launch

@WireDestination(
    style = PopUpNavigationAnimation::class,
    navArgsDelegate = VersionHistoryNavArgs::class,
)
@Composable
fun VersionHistoryScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    versionHistoryViewModel: VersionHistoryViewModel = hiltViewModel()
) {
    val optionsBottomSheetState = rememberWireModalSheetState<CellVersion>()
    val scope = rememberCoroutineScope()

    VersionHistoryScreenContent(
        versionsGroupedByTime = versionHistoryViewModel.versionsGroupedByTime.value,
        modifier = modifier,
        fileName = versionHistoryViewModel.fileName,
        versionHistoryState = versionHistoryViewModel.versionHistoryState,
        restoreDialogState = versionHistoryViewModel.restoreDialogState.value,
        navigateBack = { navigator.navigateBack() },
        optionsBottomSheetState = optionsBottomSheetState,
        restoreVersion = {
            versionHistoryViewModel.restoreVersion()
        },
        downloadVersion = {
            optionsBottomSheetState.hide()
        },
        showRestoreConfirmationDialog = { versionId ->
            optionsBottomSheetState.hide()
            versionHistoryViewModel.showRestoreConfirmationDialog(versionId)
        },
        onDismissRestoreConfirmationDialog = {
            versionHistoryViewModel.hideRestoreConfirmationDialog()
        },
        onGoToFileClicked = {},
        onRefresh = {
            scope.launch {
                versionHistoryViewModel.refreshVersions()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionHistoryScreenContent(
    versionsGroupedByTime: List<VersionGroup>,
    versionHistoryState: State<VersionHistoryState>,
    optionsBottomSheetState: WireModalSheetState<CellVersion>,
    restoreDialogState: RestoreDialogState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    fileName: String? = null,
    restoreVersion: () -> Unit = {},
    downloadVersion: (String) -> Unit = {},
    showRestoreConfirmationDialog: (String) -> Unit = {},
    onDismissRestoreConfirmationDialog: () -> Unit = {},
    onGoToFileClicked: () -> Unit = {},
    navigateBack: () -> Unit = {}
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = navigateBack,
                title = stringResource(R.string.version_history_top_appbar_title),
                subtitleContent = {
                    fileName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.wireTypography.body01,
                            color = colorsScheme().secondaryText
                        )
                    }
                },
                navigationIconType = NavigationIconType.Close(),
                elevation = dimensions().spacing0x,
            )
        },
    ) { innerPadding ->

        AnimatedVisibility(
            modifier = Modifier.padding(innerPadding),
            visible = versionHistoryState.value == VersionHistoryState.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LoadingScreen()
        }

        AnimatedVisibility(
            modifier = Modifier.padding(innerPadding),
            visible = versionHistoryState.value == VersionHistoryState.Failed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ErrorScreen(
                titleDefault = stringResource(R.string.versions_list_not_loaded_title_error),
                titleConnectionError = stringResource(R.string.versions_list_not_loaded_title_error),
                descriptionDefault = stringResource(R.string.versions_list_not_loaded_description_error),
                descriptionConnectionError = stringResource(R.string.versions_list_not_loaded_description_error),
                onRetry = onRefresh,
                modifier = Modifier.padding(innerPadding)
            )
        }

        AnimatedVisibility(
            modifier = Modifier.padding(innerPadding),
            visible = versionHistoryState.value != VersionHistoryState.Loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PullToRefreshBox(
                isRefreshing = versionHistoryState.value == VersionHistoryState.Refreshing,
                onRefresh = onRefresh,
            ) {
                LazyColumn {
                    versionsGroupedByTime.forEach { group ->
                        item(group.dateLabel.hashCode()) {
                            VersionTimeHeaderItem(group.dateLabel)
                        }
                        group.versions.forEach {
                            item(it.versionId) {
                                VersionItem(
                                    cellVersion = it,
                                    onActionClick = { cellVersion ->
                                        optionsBottomSheetState.show(cellVersion)
                                    }
                                )
                                WireDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colorsScheme().outline
                                )
                            }
                        }
                    }
                }
            }
        }

        VersionActionBottomSheet(
            sheetState = optionsBottomSheetState,
            onDismiss = { optionsBottomSheetState.hide() },
            onRestoreVersionClicked = showRestoreConfirmationDialog,
            onDownloadVersionClicked = downloadVersion
        )

        with(restoreDialogState) {
            if (visible) {
                RestoreNodeVersionConfirmationDialog(
                    restoreVersionState = restoreVersionState,
                    restoreProgress = restoreProgress,
                    onConfirm = restoreVersion,
                    onDismiss = onDismissRestoreConfirmationDialog,
                    onGoToFileClicked = onGoToFileClicked,
                )
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewVersionHistoryScreenContent() {
    WireTheme {
        VersionHistoryScreenContent(
            versionHistoryState = remember { mutableStateOf(VersionHistoryState.Idle) },
            versionsGroupedByTime = listOf(
                VersionGroup(
                    dateLabel = "Today, 3 Dec 2025".toUIText(),
                    versions = listOf(
                        CellVersion("id1", "1:46 PM", "Deniz Agha", "200MB"),
                        CellVersion("id2", "11:20 AM", "Alice Smith", "150MB"),
                        CellVersion("id3", "09:15 AM", "John Doe", "100KB"),
                        CellVersion("id4", "08:00 AM", "Eve Davis", "340KB"),
                        CellVersion("id5", "07:30 AM", "Frank Miller", "1GB"),
                    )
                ),
                VersionGroup(
                    dateLabel = "1 Dec 2025".toUIText(),
                    versions = listOf(
                        CellVersion("id6", "3:15 PM", "Bob Johnson", "300MB"),
                        CellVersion("id7", "10:05 AM", "Charlie Brown", "250KB"),
                    )
                )
            ),
            optionsBottomSheetState = rememberWireModalSheetState<CellVersion>(),
            restoreDialogState = RestoreDialogState(),
            onRefresh = {}
        )
    }
}
