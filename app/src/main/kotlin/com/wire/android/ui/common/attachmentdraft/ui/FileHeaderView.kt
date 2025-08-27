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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.icon
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DeviceUtil
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun FileHeaderView(
    extension: String,
    size: Long?,
    isMyMessage: Boolean,
    isBubble: Boolean,
    modifier: Modifier = Modifier,
    type: AttachmentFileType? = null,
    label: String? = null,
    labelColor: Color? = null,
    isError: Boolean = false,
) {
    val attachmentFileType = type ?: remember(extension) { AttachmentFileType.fromExtension(extension) }
    val sizeString = remember(size) { size?.let { DeviceUtil.formatSize(size) } ?: "" }

    val color = when {
        isBubble -> {
            if(isMyMessage) {
                colorsScheme().onPrimary
            } else {
                colorsScheme().secondaryText
            }
        }
        else -> {
            colorsScheme().secondaryText
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)
    ) {
        if (isError) {
            Icon(
                modifier = Modifier.size(dimensions().spacing16x),
                imageVector = Icons.Outlined.WarningAmber,
                tint = colorsScheme().error,
                contentDescription = null,
            )
        } else {
            Image(
                modifier = Modifier.size(dimensions().spacing16x),
                painter = painterResource(id = attachmentFileType.icon()),
                contentDescription = null,
            )
        }
        Text(
            text = "${extension.uppercase()} ($sizeString)",
            style = typography().subline01,
            color = color,
        )
        Spacer(modifier = Modifier.weight(1f))
        label?.let {
            Text(
                text = label,
                style = typography().subline01,
                color = labelColor ?: color,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewFileHeader() {
    WireTheme {
        Column(
            modifier = Modifier.padding(dimensions().spacing8x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
        ) {
            FileHeaderView(
                extension = "PDF",
                size = 1241235,
                label = "Tap to download",
                isBubble = false,
                isMyMessage = false
            )
            FileHeaderView(
                extension = "DOCX",
                size = 6796203,
                label = "Downloading...",
                isBubble = false,
                isMyMessage = false
            )
            FileHeaderView(
                extension = "ZIP",
                size = 512746,
                label = "Tap to view",
                isBubble = false,
                isMyMessage = false
            )
            FileHeaderView(
                extension = "OTHER",
                size = 78238296,
                isBubble = false,
                isMyMessage = false
            )
            FileHeaderView(
                extension = "OTHER",
                size = 78238296,
                isError = true,
                isBubble = false,
                isMyMessage = false
            )
        }
    }
}
