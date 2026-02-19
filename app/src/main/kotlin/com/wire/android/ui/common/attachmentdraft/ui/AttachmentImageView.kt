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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AttachmentImageView(
    attachment: AttachmentDraftUi,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.aspectRatio(1f),
    ) {
        AsyncImage(
            contentScale = ContentScale.Crop,
            model = attachment.localFilePath,
            contentDescription = null,
        )
        attachment.uploadProgress?.let { progress ->
            WireLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                progress = { progress },
                color = if (attachment.uploadError) colorsScheme().error else colorsScheme().primary,
                trackColor = Color.Transparent,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAttachmentDraftImageView() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                fileName = "Test Image.jpg",
                fileSize = 23462346,
                localFilePath = "",
            ),
            onClick = {},
            onMenuButtonClick = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAttachmentDraftImageViewProgress() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                fileName = "Test Image.jpg",
                fileSize = 23462346,
                localFilePath = "",
                uploadProgress = 0.75f
            ),
            onClick = {},
            onMenuButtonClick = {}
        )
    }
}
