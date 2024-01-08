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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
class WireModalSheetState(
    initialValue: SheetValue = SheetValue.Hidden,
) {
    val sheetState: SheetState = SheetState(
        skipPartiallyExpanded = true,
        initialValue = initialValue,
        confirmValueChange = { true },
        skipHiddenState = false
    )

    var currentValue: SheetValue by mutableStateOf(initialValue)
        private set

    fun show() {
        currentValue = SheetValue.Expanded
    }

    suspend fun hide() {
        sheetState.hide()
        currentValue = SheetValue.Hidden
    }

    fun onDismissRequest() {
        currentValue = SheetValue.Hidden
    }

    // When the available screen height changes, for instance when keyboard disappears, for a brief moment the sheet's content is visible
    // so change in elevation and alpha according to this flag is just to make sure that the content isn't visible until it's really needed.
    val visibleContent = sheetState.currentValue != SheetValue.Hidden || sheetState.targetValue != SheetValue.Hidden

    val isVisible
        get() = currentValue != SheetValue.Hidden

    companion object {
        fun saver() = Saver<WireModalSheetState, SheetValue>(
            save = { it.currentValue },
            restore = { savedValue -> WireModalSheetState(savedValue) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberWireModalSheetState(initialValue: SheetValue = SheetValue.Hidden): WireModalSheetState =
    rememberSaveable(saver = WireModalSheetState.saver()) {
        WireModalSheetState(initialValue)
    }
