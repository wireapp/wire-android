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

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.feature.cells.ui.moveToFolderViewModel
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.Breadcrumbs
import com.wire.android.feature.cells.ui.common.LoadingScreen
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
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

@Composable
internal fun MoveToFolderRouteScreen(
    onNavigateBack: () -> Unit,
    onCloseFlow: () -> Unit,
    onNavigateBackSteps: (Int) -> Unit,
    onOpenCreateFolder: (String) -> Unit,
    onOpenFolder: (String, List<NodeToMove>, List<String>) -> Unit,
    moveToFolderViewModel: MoveToFolderViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewState by moveToFolderViewModel.state.collectAsState()

    val isMoving = viewState.screenState == MoveToFolderScreenState.LOADING_IN_FULL_SCREEN

    // Swallow system back while moving, see MoveToFolderViewModel.isMoving.
    BackHandler(enabled = isMoving) {}

    Box(modifier = Modifier.fillMaxSize()) {
        WireScaffold(
            modifier = modifier,
            topBar = {
                TopScreenBar(
                    breadcrumbs = viewState.breadcrumbs.toTypedArray(),
                    isNavigateBackVisible = !isMoving,
                    onBreadcrumbClick = moveToFolderViewModel::onBreadcrumbClick,
                    onNavigateBack = { if (!moveToFolderViewModel.isMoving()) onNavigateBack() },
                )
            },
            bottomBar = {
                BottomScreenBar(
                    viewState = viewState,
                    onCreateFolderClick = moveToFolderViewModel::onCreateFolderClick,
                    onMoveToFolderClick = moveToFolderViewModel::onMoveToFolderClick,
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                val uiState = when {
                    viewState.screenState == MoveToFolderScreenState.LOADING_CONTENT -> ContentUiState.LOADING
                    viewState.folders.isEmpty() -> ContentUiState.EMPTY
                    else -> ContentUiState.CONTENT
                }

                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                ) { state ->
                    when (state) {
                        ContentUiState.LOADING -> LoadingScreen()
                        ContentUiState.EMPTY -> EmptyScreen(viewState.isInRootFolder)
                        ContentUiState.CONTENT -> MoveToFolderScreenContent(
                            folders = viewState.folders,
                            onFolderClick = moveToFolderViewModel::onFolderClick,
                        )
                    }
                }
            }
        }
    }

    viewState.moveFailure?.let { failure ->
        // Folder lists were already refreshed by the ViewModel, so closing the flow shows the actual result.
        MoveFailureDialog(failure = failure, onDismiss = onCloseFlow)
    }

    HandleActions(moveToFolderViewModel.actions) { action ->
        when (action) {
            is MoveToFolderViewAction.Success -> {
                Toast.makeText(context, context.moveSuccessMessage(action.movedCount, action.destinationFolderName), Toast.LENGTH_SHORT).show()
                onCloseFlow()
            }

            is MoveToFolderViewAction.NavigateToBreadcrumb -> {
                onNavigateBackSteps(action.steps)
            }

            is MoveToFolderViewAction.OpenCreateFolderScreen -> {
                onOpenCreateFolder(action.currentPath)
            }

            is MoveToFolderViewAction.OpenFolder -> {
                onOpenFolder(action.path, action.nodesToMove, action.breadcrumbs)
            }
        }
    }
}

private fun Context.moveSuccessMessage(movedCount: Int, folderName: String?): String = when {
    folderName == null && movedCount == 1 -> getString(R.string.item_move_success)
    folderName == null -> resources.getQuantityString(R.plurals.cells_items_move_success, movedCount, movedCount)
    movedCount == 1 -> getString(R.string.cells_item_move_success_to_folder, folderName)
    else -> resources.getQuantityString(R.plurals.cells_items_move_success_to_folder, movedCount, movedCount, folderName)
}

@Composable
private fun MoveFailureDialog(
    failure: MoveFailure,
    onDismiss: () -> Unit,
) {
    val isSingleItem = failure.totalCount == 1
    WireDialog(
        title = stringResource(
            if (isSingleItem) R.string.cells_move_failure_dialog_title_one else R.string.cells_move_failure_dialog_title_many
        ),
        text = stringResource(
            when {
                isSingleItem && failure.isNoNetworkConnection -> R.string.cells_move_failure_dialog_message_one_no_network
                isSingleItem -> R.string.cells_move_failure_dialog_message_one
                failure.isNoNetworkConnection -> R.string.cells_move_failure_dialog_message_many_no_network
                else -> R.string.cells_move_failure_dialog_message_many
            }
        ),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.ok_label),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@Composable
private fun TopScreenBar(
    breadcrumbs: Array<String>,
    isNavigateBackVisible: Boolean,
    onBreadcrumbClick: (Int) -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Faded out instead of removed so the title doesn't shift; hidden from accessibility while invisible.
    val navigateBackAlpha by animateFloatAsState(targetValue = if (isNavigateBackVisible) 1f else 0f)
    Column {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onNavigateBack,
            navigationIconModifier = Modifier
                .alpha(navigateBackAlpha)
                .then(if (isNavigateBackVisible) Modifier else Modifier.clearAndSetSemantics {}),
            title = stringResource(R.string.move_to_folder),
            navigationIconType = NavigationIconType.Back(),
            elevation = dimensions().spacing0x
        )
        if (breadcrumbs.isNotEmpty()) {
            Breadcrumbs(
                pathSegments = breadcrumbs,
                modifier = Modifier
                    .height(dimensions().spacing40x)
                    .fillMaxWidth(),
                onBreadcrumbsFolderClick = onBreadcrumbClick,
            )
        }
    }
}

@Composable
private fun BottomScreenBar(
    viewState: MoveToFolderViewState,
    onCreateFolderClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = viewState.screenState != MoveToFolderScreenState.LOADING_CONTENT,
        enter = fadeIn(),
        exit = fadeOut(),
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
                val isLoading = viewState.screenState == MoveToFolderScreenState.LOADING_IN_FULL_SCREEN

                if (viewState.isAllowedToMoveToCurrentPath) {
                    WireSecondaryButton(
                        text = stringResource(R.string.cells_create_folder),
                        onClick = onCreateFolderClick,
                        state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                        modifier = Modifier.padding(bottom = dimensions().spacing12x)
                    )
                } else {
                    WirePrimaryButton(
                        text = stringResource(R.string.cells_create_folder),
                        onClick = onCreateFolderClick,
                        state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                        modifier = Modifier.padding(bottom = dimensions().spacing12x)
                    )
                }

                WirePrimaryButton(
                    text = stringResource(R.string.move_here),
                    onClick = onMoveToFolderClick,
                    // `loading` only shows the spinner, the button must also be disabled to stop taps while moving.
                    state = if (viewState.isAllowedToMoveToCurrentPath && !isLoading) WireButtonState.Default else WireButtonState.Disabled,
                    loading = isLoading,
                    clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                )
            }
        }
    }
}

@Composable
private fun EmptyScreen(isRootFolder: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .padding(dimensions().spacing32x)
                .align(Alignment.Center),
            text = if (isRootFolder) {
                stringResource(R.string.no_folders_message)
            } else {
                stringResource(R.string.no_subfolders_message)
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.wireTypography.body01,
            color = colorsScheme().secondaryText,
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewMoveToFolderScreen() {
    WireTheme {
        MoveToFolderRouteScreen(
            onNavigateBack = {},
            onCloseFlow = {},
            onNavigateBackSteps = {},
            onOpenCreateFolder = {},
            onOpenFolder = { _, _, _ -> },
            moveToFolderViewModel = moveToFolderViewModel(MoveToFolderNavArgs("", emptyList())),
        )
    }
}

private enum class ContentUiState {
    LOADING,
    EMPTY,
    CONTENT
}
