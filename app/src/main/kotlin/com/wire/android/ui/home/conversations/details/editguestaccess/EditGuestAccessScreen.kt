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

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.DisableConformationDialog
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGuestAccessScreen(
    editGuestAccessViewModel: EditGuestAccessViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()

    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = scrollState.rememberTopBarElevationState().value,
            onNavigationPressed = editGuestAccessViewModel::navigateBack,
            title = stringResource(id = R.string.conversation_options_guests_label)
        )
    }) { internalPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            item {
                with(editGuestAccessViewModel) {
                    GuestOption(
                        isSwitchEnabled = editGuestAccessState.isUpdatingGuestAccessAllowed,
                        isSwitchVisible = true,
                        switchState = editGuestAccessState.isGuestAccessAllowed,
                        isLoading = editGuestAccessState.isUpdating,
                        onCheckedChange = ::updateGuestAccess
                    )
                }
            }
            item { FolderHeader(name = stringResource(id = R.string.folder_label_guest_link)) }
            item { CreateLinkItem() }
        }
    }
    with(editGuestAccessViewModel) {
        if (editGuestAccessState.changeGuestOptionConfirmationRequired) {
            DisableGuestConfirmationDialog(
                onConfirm = ::onGuestDialogConfirm,
                onDialogDismiss = ::onGuestDialogDismiss
            )
        }
    }
}

@Composable
fun CreateLinkItem() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.wireColorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = MaterialTheme.wireDimensions.spacing12x,
                    bottom = MaterialTheme.wireDimensions.spacing12x,
                    start = MaterialTheme.wireDimensions.spacing16x,
                    end = MaterialTheme.wireDimensions.spacing12x
                )
        ) {
            Text(
                text = stringResource(id = R.string.guest_link_description),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing2x)
            )
            WirePrimaryButton(
                text = stringResource(id = R.string.guest_link_button_create_link),
                fillMaxWidth = true,
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = MaterialTheme.wireDimensions.spacing16x,
                        bottom = MaterialTheme.wireDimensions.spacing16x
                    )
            )
        }
    }
}

@Composable
private fun DisableGuestConfirmationDialog(onConfirm: () -> Unit, onDialogDismiss: () -> Unit) {
    DisableConformationDialog(
        text = R.string.disable_guest_dialog_text,
        title = R.string.disable_guest_dialog_title,
        onConfirm = onConfirm,
        onDismiss = onDialogDismiss
    )
}

@Preview
@Composable
fun PreviewEditGuestAccessScreen() {
    EditGuestAccessScreen()
}

@Preview(showBackground = true)
@Composable
fun PreviewDisableGuestConformationDialog() {
    DisableGuestConfirmationDialog({}, {})
}

@Preview
@Composable
fun PreviewCreateLinkItem() {
    CreateLinkItem()
}
