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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.theme.wireDimensions

@Composable
fun CreateLinkButton(
    isLoading: Boolean,
    onCreateLink: () -> Unit
) {
    WirePrimaryButton(
        text = stringResource(id = R.string.guest_link_button_create_link),
        fillMaxWidth = true,
        onClick = onCreateLink,
        loading = isLoading,
        state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.wireDimensions.spacing16x,
                bottom = MaterialTheme.wireDimensions.spacing16x
            )
    )
}

@Composable
fun CopyLinkButton(
    onCopy: () -> Unit
) {
    WireSecondaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.wireDimensions.spacing16x,
                bottom = MaterialTheme.wireDimensions.spacing2x
            ),
        text = stringResource(id = R.string.guest_link_button_copy_link),
        fillMaxWidth = true,
        onClick = onCopy
    )
}

@Composable
fun ShareLinkButton(
    onShare: () -> Unit
) {
    WireSecondaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.wireDimensions.spacing2x,
                bottom = MaterialTheme.wireDimensions.spacing2x
            ),
        text = stringResource(id = R.string.guest_link_button_share_link),
        fillMaxWidth = true,
        onClick = onShare
    )
}

@Composable
fun RevokeLinkButton(
    isLoading: Boolean = false,
    onRevoke: () -> Unit
) {
    WireSecondaryButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MaterialTheme.wireDimensions.spacing2x,
                bottom = MaterialTheme.wireDimensions.spacing16x
            ),
        text = stringResource(id = R.string.guest_link_button_revoke_link),
        fillMaxWidth = true,
        loading = isLoading,
        state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
        onClick = onRevoke
    )
}

@Preview
@Composable
fun PreviewRevokeLinkButton() {
    RevokeLinkButton(false) {}
}

@Preview
@Composable
fun PreviewCopyLinkButton() {
    CopyLinkButton {}
}

@Preview
@Composable
fun PreviewShareLinkButton() {
    ShareLinkButton {}
}

@Preview
@Composable
fun PreviewCreateLinkButton() {
    CreateLinkButton(false) {}
}
