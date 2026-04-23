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
package com.wire.android.ui.common.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.compose.ui.R as ComposeUiR

@OptIn(ExperimentalMaterial3Api::class)
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
    properties: WireBottomSheetScaffoldProperties = WireBottomSheetScaffoldProperties(),
    content: @Composable (PaddingValues) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dismissSheet: () -> Unit = {
        scope.launch {
            if (properties.dismissToPartiallyExpanded) {
                scaffoldState.bottomSheetState.partialExpand()
            } else {
                scaffoldState.bottomSheetState.hide()
            }
        }
    }
    Box(modifier = Modifier.navigationBarsPadding()) {
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
                        Scrim(
                            properties = properties,
                            onDismissRequest = dismissSheet,
                            visible = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            },
            snackbarHost = snackbarHost,
            containerColor = containerColor,
            contentColor = contentColor,
            content = { paddingValues ->
                content(paddingValues)
                Scrim(
                    properties = properties,
                    onDismissRequest = dismissSheet,
                    visible = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
        BackHandler(enabled = properties.shouldDismissOnBackPress && scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            dismissSheet()
        }
    }
}

data class WireBottomSheetScaffoldProperties(
    val shouldDismissOnBackPress: Boolean = false,
    val shouldDismissOnClickOutside: Boolean = false,
    val dismissToPartiallyExpanded: Boolean = true, // otherwise it will dismiss to hidden state
    val scrimColor: Color = Color.Unspecified,
)

@Composable
private fun Scrim(
    properties: WireBottomSheetScaffoldProperties,
    onDismissRequest: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f)
    val closeSheet = stringResource(ComposeUiR.string.close_sheet)
    Canvas(
        modifier.let {
            if (visible && properties.shouldDismissOnClickOutside) {
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
        if (properties.scrimColor.isSpecified) {
            drawRect(color = properties.scrimColor, alpha = alpha.coerceIn(0f, 1f))
        }
    }
}
