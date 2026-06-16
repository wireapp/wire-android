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
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.image.WireImage
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview

@SuppressLint("ComposeModifierMissing")
@Composable
fun LinkPreviewCard(preview: MessageLinkPreview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        preview.displayHost?.let { host ->
            Text(
                text = host,
                style = MaterialTheme.wireTypography.body02,
                color = MaterialTheme.wireColorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    start = dimensions().spacing10x,
                    end = dimensions().spacing10x,
                    top = dimensions().spacing10x,
                    bottom = dimensions().spacing8x
                )
            )
        }

        preview.image?.assetDataPath?.let { assetDataPath ->
            val aspectRatio = preview.image
                ?.takeIf { it.assetWidth > 0 && it.assetHeight > 0 }
                ?.let { it.assetWidth.toFloat() / it.assetHeight.toFloat() }
                ?: (16f / 9f)

            WireImage(
                model = assetDataPath.toFile(),
                contentDescription = "null",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
                contentScale = ContentScale.Crop
            )
        }

        if (!preview.title.isNullOrBlank()) {
            Text(
                text = preview.title!!,
                style = MaterialTheme.wireTypography.title02,
                color = MaterialTheme.wireColorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    start = dimensions().spacing10x,
                    end = dimensions().spacing10x,
                    top = dimensions().spacing10x
                )
            )
        }
        if (!preview.summary.isNullOrBlank()) {
            Text(
                text = preview.summary!!,
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    start = dimensions().spacing10x,
                    end = dimensions().spacing10x,
                    top = dimensions().spacing4x,
                    bottom = dimensions().spacing10x
                )
            )
        }
    }
}

private val MessageLinkPreview.displayHost: String?
    get() {
        val link = permanentUrl ?: url
        val host = runCatching { Uri.parse(link).host }.getOrNull()
        return host
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
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
