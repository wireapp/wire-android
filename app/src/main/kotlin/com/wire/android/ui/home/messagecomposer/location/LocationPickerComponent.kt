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
package com.wire.android.ui.home.messagecomposer.location

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState

/**
 * Component to pick the current location to send.
 * Later can be expanded/refactored to allow to pick a location from the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerComponent(
    sheetState: WireModalSheetState = WireModalSheetState(initialValue = SheetValue.Expanded),
    onPickedLocation: (GeoLocatedAddress) -> Unit,
    onCloseLocation: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    WireModalSheetLayout(sheetState = sheetState, coroutineScope = coroutineScope) {
        MenuModalSheetContent(
            menuItems = buildList {
                add {
                    Text(text = "LocationPickerComponent")
                }
            }
        )
    }
}
