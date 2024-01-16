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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.rememberDismissibleWireModalSheetState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.openAppInfoScreen
import com.wire.android.util.orDefault
import com.wire.android.util.permission.rememberCurrentLocationFlow

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
    val sheetState = rememberDismissibleWireModalSheetState(initialValue = SheetValue.Expanded, onLocationClosed)

    val locationFlow = LocationFlow(
        onCurrentLocationPicked = { viewModel.getCurrentLocation(context) },
        onLocationDenied = viewModel::onPermissionsDenied
    )
    LaunchedEffect(Unit) {
        locationFlow.launch()
    }

    with(viewModel.state) {
        WireModalSheetLayout(sheetState = sheetState, coroutineScope = coroutineScope) {
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
                            HorizontalSpace.x4()
                            when (isLocationLoading) {
                                true -> LoadingLocation()
                                false -> LocationInformation(geoLocatedAddress = geoLocatedAddress)
                            }
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
                }
            )
        }
    }

    PermissionsDeniedDialog(
        shouldShowDialog = viewModel.state.showPermissionDeniedDialog,
        onDismiss = {
            viewModel.onPermissionsDialogDiscarded()
            onLocationClosed()
        },
        onOpenSettings = { context.openAppInfoScreen() }
    )
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
        progressColor = Color.Black,
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

// todo: this is a good candidate to refactor as a common component and unify with record audio.
@Composable
fun PermissionsDeniedDialog(
    shouldShowDialog: Boolean, // managed by vm state
    title: String = stringResource(id = R.string.app_permission_dialog_title),
    body: String = stringResource(id = R.string.location_app_permission_dialog_body),
    positiveButton: String = stringResource(id = R.string.app_permission_dialog_settings_positive_button),
    negativeButton: String = stringResource(id = R.string.app_permission_dialog_settings_negative_button),
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (shouldShowDialog) {
        WireDialog(
            title = title,
            text = body,
            onDismiss = onDismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = onDismiss,
                text = negativeButton,
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onOpenSettings,
                text = positiveButton,
                type = WireDialogButtonType.Primary,
                state = WireButtonState.Default
            )
        )
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
