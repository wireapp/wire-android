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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.orDefault
import com.wire.android.util.permission.PermissionsDeniedRequestDialog
import com.wire.android.util.permission.rememberCurrentLocationPermissionFlow

/**
 * Component to pick the current location to send.
 * Later can be expanded/refactored to allow to pick a location from the map.
 */
@Composable
fun LocationPickerComponent(
    onLocationPicked: (GeoLocatedAddress) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationPickerViewModel = hiltViewModel<LocationPickerViewModel>(),
    sheetState: WireModalSheetState<Unit> = rememberWireModalSheetState<Unit>(),
) {

    val locationFlow = rememberCurrentLocationPermissionFlow(
        onAllPermissionsGranted = viewModel::getCurrentLocation,
        onAnyPermissionDenied = { sheetState.hide() },
        onAnyPermissionPermanentlyDenied = viewModel::onPermissionPermanentlyDenied
    )

    with(viewModel.state) {
        WireModalSheetLayout(
            modifier = modifier,
            sheetState = sheetState,
        ) {
            LaunchedEffect(Unit) {
                locationFlow.launch()
            }
            WireMenuModalSheetContent(
                header = MenuModalSheetHeader.Visible(title = stringResource(R.string.location_attachment_share_title)),
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
                            HorizontalSpace.x4()
                            when (isLocationLoading) {
                                true -> LoadingLocation()
                                false -> LocationInformation(geoLocatedAddress = geoLocatedAddress)
                            }
                        }
                    }
                    add {
                        Box(
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                        ) {
                            if (showLocationSharingError) {
                                LocationErrorMessage {
                                    sheetState.hide {
                                        viewModel.onLocationSharingErrorDialogDiscarded()
                                        sheetState.hide()
                                    }
                                }
                            }
                            SendLocationButton(
                                isLocationLoading = isLocationLoading,
                                geoLocatedAddress = geoLocatedAddress,
                                onLocationPicked = onLocationPicked,
                                onLocationClosed = sheetState::hide
                            )
                        }
                    }
                }
            )

            if (showPermissionDeniedDialog) {
                PermissionsDeniedRequestDialog(
                    body = R.string.location_app_permission_dialog_body,
                    onDismiss = {
                        viewModel.onPermissionsDialogDiscarded()
                        sheetState.hide()
                    }
                )
            }
        }
    }
}

@Composable
private fun SendLocationButton(
    isLocationLoading: Boolean,
    geoLocatedAddress: GeoLocatedAddress?,
    onLocationPicked: (GeoLocatedAddress) -> Unit,
    onLocationClosed: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = dimensions().spacing16x)
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        WirePrimaryButton(
            onClick = {
                onLocationPicked(geoLocatedAddress!!)
                onLocationClosed()
            },
            leadingIcon = Icons.Filled.Send.Icon(Modifier.padding(end = dimensions().spacing8x)),
            text = stringResource(id = R.string.content_description_send_button),
            state = if (isLocationLoading || geoLocatedAddress == null) {
                WireButtonState.Disabled
            } else {
                WireButtonState.Default
            }
        )
        VerticalSpace.x16()
    }
}

@Composable
private fun LocationErrorMessage(
    message: String = stringResource(id = R.string.location_could_not_be_shared),
    onLocationClosed: () -> Unit
) {
    Box(Modifier.zIndex(Float.MAX_VALUE), contentAlignment = Alignment.BottomCenter) {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(snackbarHostState) {
            val result = snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            when (result) {
                SnackbarResult.Dismissed -> onLocationClosed()
                SnackbarResult.ActionPerformed -> {
                    /* do nothing */
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun LocationInformation(geoLocatedAddress: GeoLocatedAddress?) {
    MenuItemIcon(
        id = R.drawable.ic_location,
        contentDescription = stringResource(R.string.attachment_share_location)
    )
    HorizontalSpace.x8()
    Text(
        text = geoLocatedAddress?.getFormattedAddress()
            .orDefault(stringResource(R.string.location_loading_label)),
        modifier = Modifier.wrapContentWidth(),
        style = MaterialTheme.wireTypography.body01,
        textAlign = TextAlign.Start
    )
}

@Composable
private fun RowScope.LoadingLocation() {
    WireCircularProgressIndicator(
        progressColor = MaterialTheme.wireColorScheme.primary,
        modifier = Modifier.align(alignment = Alignment.CenterVertically)
    )
    HorizontalSpace.x8()
    Text(
        text = stringResource(R.string.location_loading_label),
        modifier = Modifier.wrapContentWidth(),
        style = MaterialTheme.wireTypography.body01,
        textAlign = TextAlign.Start
    )
}
