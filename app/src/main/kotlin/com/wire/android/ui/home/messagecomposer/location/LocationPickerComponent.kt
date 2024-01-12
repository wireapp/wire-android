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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.orDefault
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
        onCurrentLocationPicked = {
            getCurrentLocation(viewModel::onLocationPicked, context, coroutineScope)
            viewModel.setPermissionsAllowed(true)
        },
        onLocationDenied = { /*todo: show toast location could not be shared*/ }
    )
    LaunchedEffect(Unit) {
        locationFlow.launch()
    }

    with(viewModel.state) {
        if (isPermissionsAllowed) {
            WireModalSheetLayout(
                sheetState = sheetState,
                coroutineScope = coroutineScope
            ) {
                MenuModalSheetContent(
                    header = MenuModalSheetHeader.Visible(title = stringResource(R.string.attachment_share_location)),
                    menuItems = buildList {
                        add {
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = dimensions().spacing80x)
                                    .align(alignment = Alignment.Start)
                                    .padding(horizontal = dimensions().spacing16x)
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                            ) {
                                MenuItemIcon(
                                    id = R.drawable.ic_location,
                                    contentDescription = stringResource(R.string.attachment_share_location)
                                )
                                HorizontalSpace.x4()
                                Text(
                                    text = geoLocatedAddress?.getFormattedAddress()
                                        .orDefault(stringResource(R.string.settings_forgot_lock_screen_please_wait_label)), //change string
                                    modifier = Modifier.wrapContentWidth(),
                                    style = MaterialTheme.wireTypography.body01,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        add {
                            Column(
                                modifier = Modifier
                                    .align(alignment = Alignment.Start)
                                    .padding(horizontal = dimensions().spacing16x)
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                            ) {
                                WirePrimaryButton(
                                    onClick = { onLocationPicked(geoLocatedAddress!!) },
                                    leadingIcon = Icons.Filled.Send.Icon(modifier = Modifier.padding(end = 8.dp)),
                                    text = stringResource(id = R.string.content_description_send_button)
                                )
                                VerticalSpace.x16()
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
    onLocationDenied: () -> Unit,
) =
    rememberCurrentLocationFlow(
        onPermissionAllowed = onCurrentLocationPicked,
        onPermissionDenied = onLocationDenied
    )

