/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.messagecomposer

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview

@SuppressLint("ComposeModifierMissing")
@Composable
fun LinkPreviewCard(preview: MessageLinkPreview) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions().spacing8x),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(dimensions().spacing1x, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(dimensions().spacing12x)) {
            preview.image?.assetDataPath?.let { assetDataPath ->
                val aspectRatio = preview.image
                    ?.takeIf { it.assetWidth > 0 && it.assetHeight > 0 }
                    ?.let { it.assetWidth.toFloat() / it.assetHeight.toFloat() }
                    ?: (16f / 9f)

                com.wire.android.ui.common.image.WireImage(
                    model = assetDataPath.toFile(),
                    contentDescription = "null",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(RoundedCornerShape(dimensions().spacing4x))
                        .padding(bottom = dimensions().spacing8x),
                    contentScale = ContentScale.Crop
                )
            }
            if (!preview.title.isNullOrBlank()) {
                Text(
                    text = preview.title!!,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!preview.summary.isNullOrBlank()) {
                Text(
                    text = preview.summary!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = dimensions().spacing4x)
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun LinkPreviewCardPreview() = WireTheme(accent = Accent.Petrol) {
    LinkPreviewCard(
        MessageLinkPreview(
            title = "Example Link Preview Title",
            summary = "This is a summary of the link preview. It should be concise and informative.",
            url = "https://www.example.com",
            urlOffset = 0,
        )
    )
}
