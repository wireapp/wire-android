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
package com.wire.android.ui.home.cell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.common.attachmentdraft.ui.FileHeaderView
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.util.fileExtension

@Composable
fun CellListItem(
    file: AttachmentDraftUi,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(colorsScheme().surface)) {
        Column(
            modifier = Modifier.height(88.dp)
        ) {
            FileHeaderView(
                modifier = Modifier.padding(10.dp),
                extension = file.fileName.fileExtension() ?: "",
                size = file.fileSize
            )
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
                style = typography().body02,
                color = colorsScheme().onSurface,
                fontSize = 14.sp,
                maxLines = 2,
                text = file.fileName,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(file.uploadProgress != null) {
                WireLinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { file.uploadProgress ?: 0f },
                    color = if (file.uploadError) colorsScheme().error else colorsScheme().primary
                )
            }
        }
        if (file.showDraftLabel) {
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        color = colorsScheme().onPrimaryVariant,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp),
                text = "DRAFT",
                color = colorsScheme().inverseOnSurface,
                fontSize = 10.sp
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellListItem() {
    WireTheme {
        CellListItem(
            file = AttachmentDraftUi(
                uuid = "",
                fileName = "file name",
                localFilePath = "",
                fileSize = 123,
                uploadProgress = 0.5f,
                uploadError = false,
            )
        )
    }
}
