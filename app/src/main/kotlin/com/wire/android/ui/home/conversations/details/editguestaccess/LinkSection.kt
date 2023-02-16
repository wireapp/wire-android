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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun LinkSection(
    isGeneratingLink: Boolean,
    isRevokingLink: Boolean,
    link: String,
    onCreateLink: () -> Unit,
    onRevokeLink: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {
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
            Text(
                text = link,
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.guestRoomLinkTextColor,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing4x)
            )
            if (link.isEmpty()) {
                CreateLinkButton(isLoading = isGeneratingLink, onCreateLink = onCreateLink)
            } else {
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
    LinkSection(
        isGeneratingLink = false,
        isRevokingLink = false,
        link = "",
        onCreateLink = {},
        onRevokeLink = {},
        onCopyLink = {},
        onShareLink = {}
    )
}
