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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.cells.destinations.CreateFolderScreenDestination
import com.wire.android.ui.cells.destinations.MoveToFolderScreenDestination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.Breadcrumbs
import com.wire.android.feature.cells.ui.common.LoadingScreen
import com.wire.android.model.ClickBlockParams
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.PreviewNavigator
import com.wire.android.navigation.PreviewResultRecipient
import com.wire.android.navigation.WireNavigator
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

@Suppress("CyclomaticComplexMethod")
@Destination<ExternalModuleGraph>(
    navArgs = MoveToFolderNavArgs::class,
)
@Composable
fun MoveToFolderScreen(
    navigator: WireNavigator,
    createFolderResultRecipient: ResultRecipient<CreateFolderScreenDestination, Boolean>,
    modifier: Modifier = Modifier,
    moveToFolderViewModel: MoveToFolderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val viewState by moveToFolderViewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        WireScaffold(
            modifier = modifier,
            topBar = {
                TopScreenBar(
                    breadcrumbs = viewState.breadcrumbs.toTypedArray(),
                    onBreadcrumbClick = moveToFolderViewModel::onBreadcrumbClick,
                    onNavigateBack = { navigator.navigateBack() }
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

    HandleActions(moveToFolderViewModel.actions) { action ->
        when (action) {
            is MoveToFolderViewAction.Success -> {
                Toast.makeText(context, context.resources.getString(R.string.item_move_success), Toast.LENGTH_SHORT).show()
                navigator.navigateBackAndRemoveAllConsecutive(MoveToFolderScreenDestination.route)
            }

            is MoveToFolderViewAction.Failure -> {
                Toast.makeText(context, context.resources.getString(R.string.item_move_failure), Toast.LENGTH_SHORT).show()
                navigator.navigateBackAndRemoveAllConsecutive(MoveToFolderScreenDestination.route)
            }

            is MoveToFolderViewAction.NavigateToBreadcrumb -> {
                navigator.navigateBackAndRemoveAllConsecutiveXTimes(
                    currentRoute = MoveToFolderScreenDestination.route,
                    stepsBack = action.steps
                )
            }

            is MoveToFolderViewAction.OpenCreateFolderScreen -> {
                navigator.navigate(
                    NavigationCommand(
                        CreateFolderScreenDestination(action.currentPath)
                    )
                )
            }

            is MoveToFolderViewAction.OpenFolder -> {
                navigator.navigate(
                    NavigationCommand(
                        MoveToFolderScreenDestination(
                            currentPath = action.path,
                            nodeToMovePath = action.nodePath,
                            uuid = action.nodeUuid,
                            breadcrumbs = action.breadcrumbs.toTypedArray()
                        ),
                        launchSingleTop = false
                    )
                )
            }
        }
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
private fun TopScreenBar(
    breadcrumbs: Array<String>,
    onBreadcrumbClick: (Int) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onNavigateBack,
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
                    state = if (viewState.isAllowedToMoveToCurrentPath) WireButtonState.Default else WireButtonState.Disabled,
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
        MoveToFolderScreen(
            navigator = PreviewNavigator,
            createFolderResultRecipient = PreviewResultRecipient as ResultRecipient<CreateFolderScreenDestination, Boolean>
        )
    }
}

private enum class ContentUiState {
    LOADING,
    EMPTY,
    CONTENT
}
