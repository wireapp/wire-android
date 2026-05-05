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
@file:OptIn(ExperimentalMaterial3Api::class)

package com.wire.android.ui.common.bottomsheet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import androidx.compose.ui.R as ComposeUiR

@Composable
fun WireBottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetPeekHeight: Dp = BottomSheetDefaults.SheetPeekHeight,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetShape: Shape = WireBottomSheetDefaults.WireBottomSheetShape,
    sheetContainerColor: Color = WireBottomSheetDefaults.WireSheetContainerColor,
    sheetContentColor: Color = WireBottomSheetDefaults.WireSheetContentColor,
    sheetTonalElevation: Dp = WireBottomSheetDefaults.WireSheetTonalElevation,
    sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
    sheetDragHandle: @Composable (() -> Unit)? = { WireDragHandle() },
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = WireBottomSheetDefaults.WireContainerColor,
    contentColor: Color = WireBottomSheetDefaults.WireContentColor,
    sheetScrim: SheetScrimState = SheetScrimState.Hidden,
    content: @Composable (PaddingValues) -> Unit
) {
    var topInset by remember { mutableIntStateOf(0) }
    val scrimAlpha by animateFloatAsState(if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) 1f else 0f)

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .onGloballyPositioned { coordinates ->
                topInset = coordinates.positionInWindow().y.roundToInt()
            }
    ) {
        BottomSheetScaffold(
            sheetContent = sheetContent,
            modifier = modifier,
            scaffoldState = scaffoldState,
            sheetPeekHeight = sheetPeekHeight,
            sheetMaxWidth = sheetMaxWidth,
            sheetShape = sheetShape,
            sheetContainerColor = sheetContainerColor,
            sheetContentColor = sheetContentColor,
            sheetTonalElevation = sheetTonalElevation,
            sheetShadowElevation = sheetShadowElevation,
            sheetDragHandle = sheetDragHandle,
            sheetSwipeEnabled = sheetSwipeEnabled,
            topBar = topBar?.let { topBar ->
                {
                    Box {
                        topBar()
                        if (sheetScrim is SheetScrimState.Visible) {
                            Scrim(alpha = scrimAlpha, onDismissRequest = sheetScrim.onScrimClicked, modifier = Modifier.matchParentSize())
                        }
                    }
                }
            },
            snackbarHost = snackbarHost,
            containerColor = containerColor,
            contentColor = contentColor,
            content = { paddingValues ->
                content(paddingValues)
                if (sheetScrim is SheetScrimState.Visible) {
                    Scrim(alpha = scrimAlpha, onDismissRequest = sheetScrim.onScrimClicked, modifier = Modifier.fillMaxSize())
                }
            }
        )
    }
    if (sheetScrim is SheetScrimState.Visible) {
        StatusBarScrim(alpha = scrimAlpha, topInset = topInset)
    }
}

@Stable
sealed interface SheetScrimState {
    data object Hidden : SheetScrimState
    data class Visible(val onScrimClicked: (() -> Unit)? = null) : SheetScrimState
}

@Composable
private fun Scrim(
    modifier: Modifier = Modifier,
    alpha: Float = 0f,
    onDismissRequest: (() -> Unit)? = null,
) {
    val color = BottomSheetDefaults.ScrimColor
    val closeSheet = stringResource(ComposeUiR.string.close_sheet)
    Canvas(
        modifier.let {
            if (alpha > 0f && onDismissRequest != null) {
                it
                    .pointerInput(onDismissRequest) {
                        detectTapGestures { onDismissRequest() }
                    }
                    .semantics(mergeDescendants = true) {
                        traversalIndex = 1f
                        contentDescription = closeSheet
                        onClick {
                            onDismissRequest()
                            true
                        }
                    }
            } else {
                it
            }
        }
    ) {
        drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
    }
}

@Composable
private fun StatusBarScrim(alpha: Float = 0f, topInset: Int = 0) {
    if (!LocalInspectionMode.current) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(x = 0, y = -topInset),
            properties = PopupProperties(focusable = false, clippingEnabled = false)
        ) {
            Scrim(
                alpha = alpha,
                onDismissRequest = null, // status bar scrim should not be clickable
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { topInset.toDp() })
            )
        }
    }
}
