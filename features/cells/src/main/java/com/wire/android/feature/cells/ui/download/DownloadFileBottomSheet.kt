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
package com.wire.android.feature.cells.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.FileIconPreview
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadFileBottomSheet(
    file: CellNodeUi.File,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState
    ) {
        ContentView(
            file = file,
            onCancel = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismiss()
                    }
                }
            },
            onDownload = onDownload,
        )
    }
}

@Composable
private fun ContentView(
    file: CellNodeUi.File,
    onCancel: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensions().spacing24x)
    ) {
        Row(
            modifier = Modifier
                .height(dimensions().spacing64x)
                .padding(horizontal = dimensions().spacing8x)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            FileIconPreview(file)

            Text(
                text = file.name ?: "",
                style = typography().title02,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .height(dimensions().spacing120x)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {

            if (file.downloadProgress == null) {

                Text(
                    modifier = Modifier.padding(dimensions().spacing16x),
                    text = stringResource(R.string.download_file_message),
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = dimensions().spacing8x),
                    horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
                ) {

                    WireSecondaryButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        onClick = onCancel
                    )

                    WireButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.download),
                        onClick = onDownload
                    )
                }
            } else {

                Column {

                    Text(
                        modifier = Modifier.padding(dimensions().spacing16x),
                        text = stringResource(R.string.downloading_file_message),
                    )

                    WireLinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions().spacing8x),
                        progress = { file.downloadProgress },
                    )
                }
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun DownloadFileBottomSheetPreview() {
    WireTheme {
        ContentView(
            file = CellNodeUi.File(
                name = "file.txt",
                conversationName = "Conversation",
                downloadProgress = null,
                uuid = "234324",
                mimeType = "video/mp4",
                assetType = AttachmentFileType.VIDEO,
                size = 23432532532,
                localPath = null,
                userName = null,
                ownerUserId = "userId",
                userHandle = "userHandle",
                modifiedTime = null,
                remotePath = null,
                contentHash = null,
                contentUrl = null,
                previewUrl = null,
                publicLinkId = null,
            ),
            onCancel = {},
            onDownload = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun DownloadFileBottomSheetDownloadingPreview() {
    WireTheme {
        ContentView(
            file = CellNodeUi.File(
                name = "file.txt",
                conversationName = "Conversation",
                downloadProgress = 0.75f,
                uuid = "234324",
                mimeType = "video/mp4",
                assetType = AttachmentFileType.VIDEO,
                size = 23432532532,
                localPath = null,
                userName = null,
                ownerUserId = "userId",
                userHandle = "userHandle",
                modifiedTime = null,
                remotePath = null,
                contentHash = null,
                contentUrl = null,
                previewUrl = null,
                publicLinkId = null,
            ),
            onCancel = {},
            onDownload = {}
        )
    }
}
