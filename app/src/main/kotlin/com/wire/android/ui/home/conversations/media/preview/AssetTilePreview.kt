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
package com.wire.android.ui.home.conversations.media.preview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.image.WireImage
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AttachmentType
import okio.Path.Companion.toPath
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetTilePreview(
    assetBundle: AssetBundle,
    showOnlyExtension: Boolean,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .border(
                width = if (isSelected) {
                    dimensions().spacing2x
                } else {
                    dimensions().spacing1x
                },
                color = if (isSelected) {
                    MaterialTheme.wireColorScheme.primary
                } else {
                    MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline
                },
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .combinedClickable(onClick = onClick)
    ) {
        when (assetBundle.assetType) {
            AttachmentType.IMAGE -> WireImage(
                modifier = Modifier.fillMaxSize(),
                model = assetBundle.dataPath.toFile(),
                contentScale = ContentScale.Crop,
                contentDescription = assetBundle.fileName
            )

            AttachmentType.GENERIC_FILE,
            AttachmentType.AUDIO,
            AttachmentType.VIDEO -> if (showOnlyExtension) {
                AssetExtensionPreviewTile(assetBundle.assetName)
            } else {
                AssetFilePreviewTile(assetBundle, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun AssetExtensionPreviewTile(assetName: String, modifier: Modifier = Modifier) {
    Text(
        text = assetName.split(".").last().uppercase(Locale.getDefault()),
        style = MaterialTheme.wireTypography.title05,
        color = MaterialTheme.wireColorScheme.secondaryText,
        modifier = modifier
    )
}

@Composable
fun AssetFilePreviewTile(assetBundle: AssetBundle, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(dimensions().spacing8x)) {
        Text(
            modifier = Modifier.weight(1F),
            text = assetBundle.assetName,
            style = MaterialTheme.wireTypography.body02,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Row {
            Image(
                modifier = Modifier,
                painter = painterResource(R.drawable.ic_file),
                contentDescription = stringResource(R.string.content_description_image_message),
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.badge)
            )
            HorizontalSpace.x4()
            Text(
                text = assetBundle.extensionWithSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.wireColorScheme.secondaryText,
                style = MaterialTheme.wireTypography.subline01
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAssetTileItemPreview() {
    WireTheme {
        AssetFilePreviewTile(
            AssetBundle(
                "key",
                "file/pdf",
                dataPath = "some-data-path".toPath(),
                20_000,
                "long naaaaaaaaaaaaaaaaaaaaaaaaaaaaame document.pdf",
                AttachmentType.GENERIC_FILE
            ),
            modifier = Modifier
                .height(dimensions().spacing120x)
                .width(dimensions().spacing120x)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAssetTileFullWidthPreview() {
    WireTheme {
        AssetFilePreviewTile(
            AssetBundle(
                "key",
                "file/pdf",
                dataPath = "some-data-path".toPath(),
                20_000,
                "long naaaaaaaaaaaaaaaaaaaaaaaaaaaaame document.pdf",
                AttachmentType.GENERIC_FILE
            ),
            modifier = Modifier.height(dimensions().spacing120x)
        )
    }
}
