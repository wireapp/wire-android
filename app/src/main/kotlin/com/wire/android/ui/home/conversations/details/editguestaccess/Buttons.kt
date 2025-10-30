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

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.theme.wireDimensions

@Composable
fun CreateGuestLinkButton(
    enabled: Boolean,
    isLoading: Boolean,
    onCreateLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        text = stringResource(id = R.string.guest_link_button_create_link),
        fillMaxWidth = true,
        onClick = onCreateLink,
        loading = isLoading,
        state = if (!enabled) {
            WireButtonState.Disabled
        } else {
            WireButtonState.Default
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(MaterialTheme.wireDimensions.spacing16x)

    )
}

@Composable
fun CopyLinkButton(
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing16x,
                top = MaterialTheme.wireDimensions.spacing16x,
                bottom = MaterialTheme.wireDimensions.spacing4x
            ),
        text = stringResource(id = R.string.guest_link_button_copy_link),
        fillMaxWidth = true,
        onClick = onCopy
    )
}

@Composable
fun ShareLinkButton(
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing16x,
                top = MaterialTheme.wireDimensions.spacing4x,
                bottom = MaterialTheme.wireDimensions.spacing4x
            ),
        text = stringResource(id = R.string.guest_link_button_share_link),
        fillMaxWidth = true,
        onClick = onShare
    )
}

@Composable
fun RevokeLinkButton(onRevoke: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    WireSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing16x,
                top = MaterialTheme.wireDimensions.spacing4x,
                bottom = MaterialTheme.wireDimensions.spacing12x
            ),
        colors = wireSecondaryButtonColors(),
        text = stringResource(id = R.string.guest_link_button_revoke_link),
        fillMaxWidth = true,
        loading = isLoading,
        state = if (isLoading) WireButtonState.Disabled else WireButtonState.Error,
        onClick = onRevoke
    )
}

@Preview
@Composable
fun PreviewRevokeLinkButton() {
    RevokeLinkButton({}, isLoading = false)
}

@Preview
@Composable
fun PreviewCopyLinkButton() {
    CopyLinkButton({})
}

@Preview
@Composable
fun PreviewShareLinkButton() {
    ShareLinkButton({})
}

@Preview
@Composable
fun PreviewCreateLinkButton() {
    CreateGuestLinkButton(enabled = true, isLoading = false, onCreateLink = {})
}
