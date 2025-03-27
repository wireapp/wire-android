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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.wire.android.R
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.colorsScheme
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
        attachment.uploadProgress?.let {
            WireLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                progress = { attachment.uploadProgress },
                color = if (attachment.uploadError) colorsScheme().error else colorsScheme().primary,
                trackColor = Color.Transparent,
            )
        }
        Image(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center),
            painter = painterResource(R.drawable.ic_play_circle_filled),
            contentDescription = null,
        )
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
            onClickDelete = {}
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
            onClickDelete = {}
        )
    }
}
