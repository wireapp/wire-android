/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.common.attachmentdraft.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AttachmentScaffold(
    onClick: () -> Unit,
    onMenuButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMenuButton: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .height(dimensions().attachmentDraftHeight)
                .padding(dimensions().spacing8x)
                .border(
                    width = dimensions().spacing1x,
                    color = colorsScheme().secondaryButtonEnabledOutline,
                    shape = RoundedCornerShape(dimensions().buttonCornerSize)
                )
                .background(
                    color = colorsScheme().surface,
                    shape = RoundedCornerShape(dimensions().buttonCornerSize)
                )
                .clip(RoundedCornerShape(dimensions().buttonCornerSize))
                .clickable { onClick() }
        ) {
            content()
        }
        Icon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .clickable { onMenuButtonClick() }
                .background(
                    color = colorsScheme().surface,
                    shape = CircleShape
                )
                .border(
                    width = dimensions().spacing1x,
                    color = colorsScheme().secondaryButtonEnabledOutline,
                    shape = CircleShape
                )
                .padding(dimensions().spacing4x),
            painter = painterResource(
                if (showMenuButton) R.drawable.ic_more_horiz else R.drawable.ic_close,
            ),
            contentDescription = null,
            tint = colorsScheme().onSurface,
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAttachmentScaffold() {
    WireTheme {
        AttachmentScaffold(
            onClick = {},
            onMenuButtonClick = {},
        ) {
            Column {
                // Attachment content
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAttachmentMenuScaffold() {
    WireTheme {
        AttachmentScaffold(
            onClick = {},
            onMenuButtonClick = {},
            showMenuButton = true,
        ) {
            Column {
                // Attachment content
            }
        }
    }
}
