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
import com.wire.android.ui.common.bottomsheet.WireModalSheetState

@OptIn(ExperimentalMaterial3Api::class)
data class LocationPickerState(
    val geoLocatedAddress: GeoLocatedAddress? = null,
    val isLocationLoading: Boolean = false,
    val isPermissionDiscarded: Boolean = false,
    val showPermissionDeniedDialog: Boolean = false,
    val wireModalSheetState: WireModalSheetState = WireModalSheetState(SheetValue.Hidden)
)
