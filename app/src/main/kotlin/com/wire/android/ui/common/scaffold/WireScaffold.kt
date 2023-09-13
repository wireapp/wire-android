/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.common.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.snackbar.LocalSnackbarHostState

/**
 * A custom scaffold that automatically applies system UI insets and IME (Input Method Editor)
 * paddings. This scaffold is particularly useful for apps that have transitioned from using
 * `adjustResize` to `adjustNothing` in their activities, ensuring that UI elements are positioned
 * correctly and do not overlap with system UI or the soft keyboard.
 *
 * @param topBar A composable function that describes the content of the top bar. This content
 *               will automatically receive padding for the status bar.
 * @param bottomBar A composable function that describes the content of the bottom bar. This content
 *                  will automatically receive padding for the navigation bar.
 * @param snackbarHost A composable function for hosting the snackbars. It doesn't receive any automatic padding.
 * @param floatingActionButton A composable function that describes the content of the floating action button.
 * @param floatingActionButtonPosition The position for the floating action button.
 * @param containerColor The background color for the scaffold. Defaults to the current theme's background color.
 * @param contentColor The preferred content color provided by this scaffold. Defaults to the appropriate
 *                     content color for the provided [containerColor].
 * @param content A composable function that describes the main content of the scaffold. This content
 *                will automatically receive padding for system UI insets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WireScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
//    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            if (topBar != null) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding(),
                ) {
                    topBar()
                }
            }
        },
        bottomBar = {
            if (bottomBar != null) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                ) {
                    bottomBar()
                }
            }
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = LocalSnackbarHostState.current,
                // added navigation padding because our WireScaffold has zero bottom WindowInsets
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            )
        },
        floatingActionButton,
        floatingActionButtonPosition,
        containerColor,
        contentColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding)
                .systemBarsPadding()
        ) {
            content(padding)
        }
    }
}
