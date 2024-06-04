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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Deprecated("Use WireModalSheetLayout2")
fun WireModalSheetLayout(
    sheetState: WireModalSheetState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    sheetShape: Shape = WireBottomSheetDefaults.WireBottomSheetShape,
    containerColor: Color = WireBottomSheetDefaults.WireSheetContainerColor,
    contentColor: Color = WireBottomSheetDefaults.WireSheetContentColor,
    tonalElevation: Dp = WireBottomSheetDefaults.WireSheetTonalElevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { WireBottomSheetDefaults.WireDragHandle() },
    sheetContent: @Composable ColumnScope.() -> Unit
) {
    if (sheetState.currentValue != SheetValue.Hidden) {
        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            shape = sheetShape,
            content = sheetContent,
            containerColor = containerColor,
            contentColor = contentColor,
            scrimColor = scrimColor,
            tonalElevation = tonalElevation,
            onDismissRequest = sheetState::onDismissRequest,
            dragHandle = dragHandle,
            modifier = modifier.absoluteOffset(y = 1.dp)
        )
    }

    BackHandler(enabled = sheetState.isVisible) {
        coroutineScope.launch { sheetState.hide() }
    }
}

@Composable
fun MenuModalSheetLayout(
    sheetState: WireModalSheetState,
    coroutineScope: CoroutineScope,
    menuItems: List<@Composable () -> Unit>,
    header: MenuModalSheetHeader = MenuModalSheetHeader.Gone
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = coroutineScope,
        sheetContent = {
            MenuModalSheetContent(
                menuItems = menuItems,
                header = header
            )
        }
    )
}

@Composable
fun MenuModalSheetContent(
    menuItems: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    header: MenuModalSheetHeader = MenuModalSheetHeader.Gone
) {
    Column(modifier = modifier) {
        ModalSheetHeaderItem(header = header)
        buildMenuSheetItems(items = menuItems)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireModalSheetLayout2(
    sheetState: SheetState,
    coroutineScope: CoroutineScope,
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetShape: Shape = WireBottomSheetDefaults.WireBottomSheetShape,
    containerColor: Color = WireBottomSheetDefaults.WireSheetContainerColor,
    contentColor: Color = WireBottomSheetDefaults.WireSheetContentColor,
    tonalElevation: Dp = WireBottomSheetDefaults.WireSheetTonalElevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { WireBottomSheetDefaults.WireDragHandle() },
    onCloseBottomSheet: () -> Unit
) {
    ModalBottomSheet(
        sheetState = sheetState,
        shape = sheetShape,
        content = sheetContent,
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = scrimColor,
        tonalElevation = tonalElevation,
        onDismissRequest = {
            coroutineScope.launch {
                sheetState.hide()
            }
        },
        dragHandle = dragHandle,
        modifier = modifier.absoluteOffset(y = 1.dp)
    )

    BackHandler(enabled = sheetState.isVisible) {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onCloseBottomSheet()
            }
        }
    }
}
