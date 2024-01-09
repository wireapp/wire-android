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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun GuestLinkActionButtons(
    shouldDisableGenerateGuestLinkButton: Boolean,
    isGeneratingLink: Boolean,
    isRevokingLink: Boolean,
    link: String?,
    onCreateLink: () -> Unit,
    onRevokeLink: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {

    if (link.isNullOrEmpty()) {
        CreateGuestLinkButton(
            enabled = !shouldDisableGenerateGuestLinkButton,
            isLoading = isGeneratingLink,
            onCreateLink = onCreateLink
        )
    } else {
        Column {
            CopyLinkButton(onCopyLink)
            ShareLinkButton(onShareLink)
            RevokeLinkButton(isLoading = isRevokingLink, onRevoke = onRevokeLink)
        }
    }
}

@Preview
@Composable
fun PreviewLinkSection() {
    GuestLinkActionButtons(
        shouldDisableGenerateGuestLinkButton = false,
        isGeneratingLink = false,
        isRevokingLink = false,
        link = "123",
        onCreateLink = {},
        onRevokeLink = {},
        onCopyLink = {},
        onShareLink = {}
    )
}
