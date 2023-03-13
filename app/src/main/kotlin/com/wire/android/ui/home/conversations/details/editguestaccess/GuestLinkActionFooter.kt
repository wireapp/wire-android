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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun GuestLinkActionFooter(
    shouldDisableGenerateGuestLinkButton: Boolean,
    isGeneratingLink: Boolean,
    isRevokingLink: Boolean,
    link: String?,
    onCreateLink: () -> Unit,
    onRevokeLink: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {

    Surface(
        modifier = Modifier
            .background(MaterialTheme.wireColorScheme.background),
        shadowElevation = dimensions().spacing8x
    ) {
        if (link.isNullOrEmpty()) {
            CreateLinkButton(
                shouldDisableGenerateGuestLinkButton = shouldDisableGenerateGuestLinkButton,
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
}

@Preview
@Composable
fun PreviewLinkSection() {
    GuestLinkActionFooter(
        shouldDisableGenerateGuestLinkButton = false,
        isGeneratingLink = false,
        isRevokingLink = false,
        link = "",
        onCreateLink = {},
        onRevokeLink = {},
        onCopyLink = {},
        onShareLink = {}
    )
}
