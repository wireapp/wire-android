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

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.dimensions
import com.wire.android.util.permission.rememberCurrentLocationFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Component to pick the current location to send.
 * Later can be expanded/refactored to allow to pick a location from the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerComponent(
    onLocationPicked: (GeoLocatedAddress) -> Unit,
    onLocationClosed: () -> Unit
) {
    val viewModel = hiltViewModel<LocationPickerViewModel>()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberWireModalSheetState(initialValue = SheetValue.Expanded)

    val locationFlow = LocationFlow(
        onCurrentLocationPicked = { viewModel.setPermissionsAllowed(true) },
        context,
        coroutineScope
    )
    LaunchedEffect(Unit) {
        locationFlow.launch()
    }

    with(viewModel.state) {
        if (isPermissionsAllowed) {
            getCurrentLocation(viewModel::onLocationPicked, context, coroutineScope)
            WireModalSheetLayout(
                sheetState = sheetState,
                coroutineScope = coroutineScope
            ) {
                MenuModalSheetContent(
                    menuItems = buildList {
                        add {
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = dimensions().spacing200x)
                                    .align(alignment = androidx.compose.ui.Alignment.CenterHorizontally)
                                    .fillMaxWidth()
                            ) {
                                Text(text = locationName, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible) {
            onLocationClosed()
        }
    }
}

/**
 * Choosing the best location estimate by docs.
 * https://developer.android.com/develop/sensors-and-location/location/retrieve-current#BestEstimate
 */
@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    onCurrentLocationPicked: (GeoLocatedAddress) -> Unit,
    context: Context,
    coroutineScope: CoroutineScope
) {
    val locationProvider = LocationServices.getFusedLocationProviderClient(context)
    coroutineScope.launch {
        val currentLocation = locationProvider.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token,
        ).await()
        val address = Geocoder(context).getFromLocation(currentLocation.latitude, currentLocation.longitude, 1).orEmpty()
        onCurrentLocationPicked(GeoLocatedAddress(address.firstOrNull(), currentLocation))
    }
}

@Composable
private fun LocationFlow(
    onCurrentLocationPicked: () -> Unit,
    context: Context,
    coroutineScope: CoroutineScope
) =
    rememberCurrentLocationFlow(
        onPermissionAllowed = onCurrentLocationPicked,
        onPermissionDenied = {}//todo show dialog error.
    )

