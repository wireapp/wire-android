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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> WireModalSheetLayout(
    sheetState: WireModalSheetState<T>,
    modifier: Modifier = Modifier,
    sheetShape: Shape = WireBottomSheetDefaults.WireBottomSheetShape,
    containerColor: Color = WireBottomSheetDefaults.WireSheetContainerColor,
    contentColor: Color = WireBottomSheetDefaults.WireSheetContentColor,
    tonalElevation: Dp = WireBottomSheetDefaults.WireSheetTonalElevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    onBackPress: (() -> Unit) = { sheetState.hide() },
    onDismissRequest: (() -> Unit) = sheetState::onDismissRequest,
    shouldDismissOnBackPress: Boolean = true,
    dragHandle: @Composable (() -> Unit)? = { WireBottomSheetDefaults.WireDragHandle() },
    sheetContent: @Composable ColumnScope.(T) -> Unit
) {
    (sheetState.currentValue as? WireSheetValue.Expanded<T>)?.let { expandedValue ->
        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            shape = sheetShape,
            content = {
                BackHandler(!shouldDismissOnBackPress) {
                    onBackPress()
                }
                var contentHeight: Int by remember { mutableStateOf(0) }
                Column(
                    modifier = Modifier
                        .onSizeChanged {
                            contentHeight = it.height
                        }
                ) {
                    sheetContent(expandedValue.value)
                }
                LaunchedEffect(contentHeight) {
                    sheetState.sheetState.show() // ensure the sheet height is readjusted after content height change
                }
            },
            containerColor = containerColor,
            contentColor = contentColor,
            scrimColor = scrimColor,
            tonalElevation = tonalElevation,
            onDismissRequest = onDismissRequest,
            dragHandle = dragHandle,
            modifier = modifier
                .absoluteOffset(y = 1.dp)
                .statusBarsPadding(),
            contentWindowInsets = { WindowInsets.navigationBars },
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = shouldDismissOnBackPress)
        )
    }
}

@Composable
fun WireMenuModalSheetContent(
    menuItems: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    header: MenuModalSheetHeader = MenuModalSheetHeader.Gone
) {
    Column(modifier = modifier) {
        ModalSheetHeaderItem(header = header)
        BuildMenuSheetItems(items = menuItems)
    }
}
