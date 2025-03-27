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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.util.fileExtension

@Composable
fun AttachmentFileView(
    attachment: AttachmentDraftUi,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column {
            FileHeaderView(
                modifier = Modifier.padding(dimensions().spacing10x),
                extension = attachment.fileName.fileExtension() ?: "",
                size = attachment.fileSize,
                isError = attachment.uploadError,
            )
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing12x)
                    .fillMaxWidth(),
                style = typography().body02,
                color = colorsScheme().onSurface,
                maxLines = 2,
                text = attachment.fileName,
            )
            Spacer(modifier = Modifier.height(dimensions().spacing10x))
        }

        val progress = if (attachment.uploadError) 1f else attachment.uploadProgress

        progress?.let {
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
private fun PreviewAttachmentDraftFileView() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                versionId = "123",
                fileName = "CDR_20220120 Accessibility Report Reviewed Final Plus.doc",
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
private fun PreviewAttachmentDraftViewWithProgress() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                versionId = "123",
                fileName = "CDR_20220120 Accessibility Report Reviewed Final Plus.doc",
                fileSize = 23462346,
                uploadProgress = 0.75f,
                localFilePath = "",
            ),
            onClick = {},
            onMenuButtonClick = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewAttachmentDraftViewWithError() {
    WireTheme {
        AttachmentDraftView(
            attachment = AttachmentDraftUi(
                uuid = "123",
                versionId = "123",
                fileName = "CDR_20220120 Accessibility Report Reviewed Final Plus.doc",
                fileSize = 23462346,
                uploadProgress = 0.75f,
                localFilePath = "",
                uploadError = true,
            ),
            onClick = {},
            onMenuButtonClick = {}
        )
    }
}
