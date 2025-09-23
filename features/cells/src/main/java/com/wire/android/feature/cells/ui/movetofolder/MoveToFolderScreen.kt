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
package com.wire.android.feature.cells.ui.movetofolder

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.FileIconPreview
import com.wire.android.feature.cells.ui.FolderIconPreview
import com.wire.android.feature.cells.ui.common.Breadcrumbs
import com.wire.android.feature.cells.ui.common.LoadingScreen
import com.wire.android.feature.cells.ui.destinations.CreateFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.MoveToFolderScreenDestination
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.model.ClickBlockParams
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.PreviewNavigator
import com.wire.android.navigation.PreviewResultRecipient
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@WireDestination(
    navArgsDelegate = MoveToFolderNavArgs::class,
)
@Composable
fun MoveToFolderScreen(
    navigator: WireNavigator,
    createFolderResultRecipient: ResultRecipient<CreateFolderScreenDestination, Boolean>,
    modifier: Modifier = Modifier,
    moveToFolderViewModel: MoveToFolderViewModel = hiltViewModel()
) {

    val isScreenLoading = remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        moveToFolderViewModel.loadFolders()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        WireScaffold(
            modifier = modifier,
            topBar = {
                Column {
                    WireCenterAlignedTopAppBar(
                        onNavigationPressed = { navigator.navigateBack() },
                        title = stringResource(R.string.move_to_folder),
                        navigationIconType = NavigationIconType.Back(),
                        elevation = dimensions().spacing0x
                    )
                    if (moveToFolderViewModel.breadcrumbs().isNotEmpty()) {
                        Breadcrumbs(
                            pathSegments = moveToFolderViewModel.breadcrumbs(),
                            modifier = Modifier
                                .height(dimensions().spacing40x)
                                .fillMaxWidth(),
                            onBreadcrumbsFolderClick = {
                                val stepsBack = moveToFolderViewModel.breadcrumbs().size - it - 1
                                navigator.navigateBackAndRemoveAllConsecutiveXTimes(MoveToFolderScreenDestination.route, stepsBack)
                            },
                        )
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Surface(
                        color = MaterialTheme.wireColorScheme.background,
                        shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(dimensions().spacing16x)
                        ) {
                            val isLoading = moveToFolderViewModel.state.collectAsState().value ==
                                    MoveToFolderScreenState.LOADING_IN_FULL_SCREEN

                            WireSecondaryButton(
                                text = stringResource(R.string.cells_create_folder),
                                onClick = {
                                    navigator.navigate(
                                        NavigationCommand(
                                            CreateFolderScreenDestination(moveToFolderViewModel.currentPath())
                                        )
                                    )
                                },
                                state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                                modifier = Modifier.padding(bottom = dimensions().spacing12x)
                            )

                            WirePrimaryButton(
                                text = stringResource(R.string.move_here),
                                onClick = {
                                    isScreenLoading.value = true
                                    moveToFolderViewModel.moveHere()
                                },
                                state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                                loading = isLoading,
                                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->

            val folders = moveToFolderViewModel.folders.collectAsState()
            val uiState by moveToFolderViewModel.state.collectAsState()

            MoveToFolderScreenContent(
                folders = folders,
                uiState = uiState,
                onFolderClick = { folder ->
                    navigator.navigate(
                        NavigationCommand(
                            MoveToFolderScreenDestination(
                                currentPath = "${moveToFolderViewModel.currentPath()}/${folder.name}",
                                nodeToMovePath = moveToFolderViewModel.nodeToMovePath(),
                                uuid = moveToFolderViewModel.nodeUuid(),
                                breadcrumbs = folder.name?.let { moveToFolderViewModel.breadcrumbs() + it } ?: emptyArray()
                            ),
                            launchSingleTop = false
                        )
                    )
                },
                innerPadding = innerPadding
            )
        }
    }

    HandleActions(moveToFolderViewModel.actions) { action ->
        when (action) {
            is MoveToFolderViewAction.Success ->
                Toast.makeText(context, context.resources.getString(R.string.item_move_success), Toast.LENGTH_SHORT).show()

            is MoveToFolderViewAction.Failure ->
                Toast.makeText(context, context.resources.getString(R.string.item_move_failure), Toast.LENGTH_SHORT).show()
        }
        navigator.navigateBackAndRemoveAllConsecutive(MoveToFolderScreenDestination.route)
    }

    createFolderResultRecipient.onNavResult { result ->
        when (result) {
            NavResult.Canceled -> {}
            is NavResult.Value -> {
                if (result.value) {
                    moveToFolderViewModel.loadFolders()
                }
            }
        }
    }
}

@Composable
private fun MoveToFolderScreenContent(
    folders: State<List<CellNodeUi>>,
    uiState: MoveToFolderScreenState,
    onFolderClick: (CellNodeUi.Folder) -> Unit,
    innerPadding: PaddingValues
) {

    if (uiState == MoveToFolderScreenState.LOADING_CONTENT) {
        LoadingScreen()
    } else {
        if (folders.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_subfolders_message),
                    style = MaterialTheme.wireTypography.body01,
                    color = colorsScheme().secondaryText,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(dimensions().spacing2x)
            ) {
                folders.value.forEach { node ->
                    item {
                        RowItem(node) {
                            onFolderClick(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowItem(
    cell: CellNodeUi,
    onFolderClick: (CellNodeUi.Folder) -> Unit = { }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing56x)
            .background(colorsScheme().surface)
            .padding(
                bottom = dimensions().spacing2x,
                end = dimensions().spacing12x,
            )
            .alpha(if (cell is CellNodeUi.Folder) 1f else 0.5f)
            .then(
                if (cell is CellNodeUi.Folder) {
                    Modifier.clickable { onFolderClick(cell) }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (cell) {
            is CellNodeUi.File -> FileIconPreview(cell)
            is CellNodeUi.Folder -> FolderIconPreview(cell)
        }
        Text(
            cell.name ?: "",
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
        if (cell is CellNodeUi.Folder) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
                tint = colorsScheme().onSurface,
                contentDescription = null,
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewMoveToFolderItem() {
    WireTheme {
        RowItem(
            CellNodeUi.Folder(
                uuid = "243567990900989897",
                name = "some folder.pdf",
                userName = "User",
                conversationName = "Conversation",
                modifiedTime = null,
                size = 1234,
                publicLinkId = "public"
            )
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewMoveToFolderScreen() {
    WireTheme {
        MoveToFolderScreen(
            navigator = PreviewNavigator,
            createFolderResultRecipient = PreviewResultRecipient as ResultRecipient<CreateFolderScreenDestination, Boolean>
        )
    }
}
