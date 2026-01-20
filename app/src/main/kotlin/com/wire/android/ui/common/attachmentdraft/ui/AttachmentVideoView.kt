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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.wire.android.R
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AttachmentVideoView(
    attachment: AttachmentDraftUi,
    modifier: Modifier = Modifier,
) {

    val context = LocalContext.current

    Box(
        modifier = modifier.aspectRatio(1f),
    ) {

        val model = ImageRequest.Builder(context)
            .data(attachment.localFilePath)
            .decoderFactory { result, options, _ ->
                VideoFrameDecoder(result.source, options)
            }
            .build()

        AsyncImage(
            model = model,
            contentScale = ContentScale.Crop,
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
        if (attachment.uploadError) {
            Icon(
                modifier = Modifier
                    .size(dimensions().spacing32x)
                    .align(Alignment.Center)
                    .background(
                        color = colorsScheme().surface,
                        shape = CircleShape,
                    )
                    .border(
                        width = dimensions().spacing1x,
                        color = colorsScheme().outline,
                        shape = CircleShape,
                    )
                    .padding(dimensions().spacing6x),
                painter = painterResource(commonR.drawable.ic_warning_amber),
                tint = colorsScheme().error,
                contentDescription = null,
            )
        } else {
            Image(
                modifier = Modifier
                    .size(dimensions().spacing32x)
                    .align(Alignment.Center),
                painter = painterResource(R.drawable.ic_play_circle_filled),
                contentDescription = null,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAttachmentDraftVideoView() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                fileName = "Test Image.mp4",
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
private fun PreviewAttachmentDraftVideoViewProgress() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                fileName = "Test Image.mp4",
                fileSize = 23462346,
                localFilePath = "",
                uploadProgress = 0.75f
            ),
            onClick = {},
            onMenuButtonClick = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAttachmentDraftVideoViewError() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                fileName = "Test Image.mp4",
                fileSize = 23462346,
                localFilePath = "",
                uploadProgress = 0.75f,
                uploadError = true,
            ),
            onClick = {},
            onMenuButtonClick = {}
        )
    }
}
