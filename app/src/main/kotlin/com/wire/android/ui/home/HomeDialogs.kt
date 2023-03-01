/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.CustomTabsHelper

@Composable
fun FileRestrictionDialog(
    featureFlagState: FeatureFlagState,
    hideDialogStatus: () -> Unit,
) {
    if (featureFlagState.showFileSharingDialog) {
        val text: String = if (featureFlagState.isFileSharingEnabledState) {
            stringResource(id = R.string.sharing_files_enabled)
        } else {
            stringResource(id = R.string.sharing_files_disabled)
        }

        WireDialog(
            title = stringResource(id = R.string.team_settings_changed),
            text = text,
            onDismiss = hideDialogStatus,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = hideDialogStatus,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
            )
        )
    }
}

@Composable
fun GuestRoomLinkFeatureFlagDialog(
    isGuestRoomLinkEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val text: String = if (isGuestRoomLinkEnabled) {
        stringResource(id = R.string.guest_room_link_enabled)
    } else {
        stringResource(id = R.string.guest_room_link_disabled)
    }

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun WelcomeNewUserDialog(
    dismissDialog: () -> Unit,
    context: Context = LocalContext.current
) {
    val welcomeToNewAndroidUrl = stringResource(id = R.string.url_welcome_to_new_android)
    WireDialog(
        title = stringResource(id = R.string.welcome_migration_dialog_title),
        text = stringResource(id = R.string.welcome_migration_dialog_content),
        onDismiss = dismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                dismissDialog.invoke()
                CustomTabsHelper.launchUrl(context, welcomeToNewAndroidUrl)
            },
            text = stringResource(id = R.string.label_learn_more),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = dismissDialog,
            text = stringResource(id = R.string.welcome_migration_dialog_continue),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Preview
@Composable
fun previewFileRestrictionDialog() {
    FileRestrictionDialog(FeatureFlagState()) {}
}

@Preview
@Composable
fun previewGuestRoomLinkFeatureFlagDialog() {
    GuestRoomLinkFeatureFlagDialog(true) {}
}

@Preview
@Composable
fun previewWelcomeNewUserDialog() {
    WelcomeNewUserDialog({})
}
