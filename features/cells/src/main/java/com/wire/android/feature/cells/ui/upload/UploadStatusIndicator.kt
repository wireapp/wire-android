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
package com.wire.android.feature.cells.ui.upload

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.cells.domain.CellUploadItem
import com.wire.kalium.cells.domain.CellUploadRequest
import com.wire.kalium.cells.domain.CellUploadState
import com.wire.kalium.cells.domain.isActive
import okio.Path.Companion.toPath
import com.wire.android.ui.common.R as commonR

/**
 * Compact persistent bar shown while a Shared Drive upload batch exists, so the state stays visible
 * while the user browses away from [UploadStatusBottomSheet]. Reads [uploads] straight from
 * [UploadStatusViewModel], which itself only forwards `CellUploadCoordinator.uploads` — this component
 * owns no upload state of its own. Tapping it opens the full [UploadStatusBottomSheet].
 *
 * The dismiss action clears every finished upload from [uploads] via `CellUploadCoordinator.dismiss`, so
 * this bar disappears the same way it would if the batch had simply finished and been cleared on its own —
 * no separate hidden/dismissed state to keep in sync, and it survives closing and reopening the screen
 * this bar lives on for free, since [uploads] does too. It's only offered once nothing is active, so it
 * never has to special-case dismissing a batch that's still uploading.
 */

@Composable
internal fun UploadStatusIndicator(
    uploads: List<CellUploadItem>,
    onDismissAll: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uploads.isEmpty()) return

    val isActive = uploads.any { it.state.isActive }
    val failedCount = uploads.count { it.state is CellUploadState.Failed }
    val iconState = when {
        isActive -> UploadStatusIconState.Loading
        failedCount > 0 -> UploadStatusIconState.Failed
        else -> UploadStatusIconState.Success
    }
    val title = if (!isActive && failedCount > 0) {
        stringResource(R.string.cells_upload_status_title_failed_partial, failedCount, uploads.size)
    } else {
        uploadStatusTitle(uploads)
    }

    val shape = RoundedCornerShape(dimensions().corner12x)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing8x, vertical = dimensions().spacing8x),
        shape = shape,
        color = colorsScheme().onPrimary,
        border = BorderStroke(dimensions().spacing1x, colorsScheme().outline),
        shadowElevation = dimensions().spacing6x,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimensions().spacing16x,
                vertical = dimensions().spacing12x
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
        ) {
            AnimatedContent(
                targetState = iconState,
                contentKey = { it::class.simpleName },
                transitionSpec = {
                    (scaleIn(initialScale = 0.72f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.72f) + fadeOut())
                },
                label = "cell_icon_transition",
            ) { state ->
                when (state) {
                    UploadStatusIconState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(dimensions().spacing20x),
                        color = colorsScheme().primary,
                        trackColor = colorsScheme().primaryVariant,
                        strokeWidth = dimensions().spacing2x,
                        strokeCap = StrokeCap.Round,
                    )

                    UploadStatusIconState.Failed -> Icon(
                        painter = painterResource(commonR.drawable.ic_warning_amber),
                        contentDescription = null,
                        tint = colorsScheme().error,
                        modifier = Modifier.size(dimensions().spacing20x),
                    )

                    UploadStatusIconState.Success -> Icon(
                        painter = painterResource(R.drawable.ic_checkmark_in_circle),
                        contentDescription = null,
                        tint = colorsScheme().positive,
                        modifier = Modifier.size(dimensions().spacing20x),
                    )
                }
            }
            Text(
                text = title,
                style = typography().body02,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!isActive) {
                IconButton(
                    onClick = onDismissAll,
                    modifier = Modifier.size(dimensions().spacing32x),
                ) {
                    Icon(
                        painter = painterResource(commonR.drawable.ic_close),
                        contentDescription = stringResource(R.string.content_description_dismiss_upload),
                        tint = colorsScheme().secondaryText,
                        modifier = Modifier.size(dimensions().spacing16x),
                    )
                }
            }
            Icon(
                painter = painterResource(commonR.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.content_description_open_upload_status),
                tint = colorsScheme().secondaryText,
                modifier = Modifier
                    .padding(top = dimensions().spacing8x, bottom = dimensions().spacing8x)
                    .size(dimensions().spacing16x),
            )
        }
    }
}

private sealed interface UploadStatusIconState {
    data object Loading : UploadStatusIconState
    data object Failed : UploadStatusIconState
    data object Success : UploadStatusIconState
}

@PreviewMultipleThemes
@Composable
private fun UploadStatusIndicatorPreview() {
    WireTheme {
        UploadStatusIndicator(
            uploads = listOf(
                CellUploadItem(
                    id = "1",
                    conversationId = "cellName",
                    request = CellUploadRequest(
                        localPath = "/path/to/file1.txt".toPath(),
                        fileName = "file1.txt",
                        sizeBytes = 1024,
                        destinationFolderPath = "cellName/folder",
                    ),
                    state = CellUploadState.Queued,
                    nodeUuid = "node-uuid-1",
                    versionId = "version-id-1",
                ),
                CellUploadItem(
                    id = "2",
                    conversationId = "cellName",
                    request = CellUploadRequest(
                        localPath = "/path/to/file2.txt".toPath(),
                        fileName = "file2.txt",
                        sizeBytes = 1024,
                        destinationFolderPath = "cellName/folder",
                    ),
                    state = CellUploadState.Uploading(50f),
                    nodeUuid = "node-uuid-2",
                    versionId = "version-id-2",
                ),
            ),
            onDismissAll = {},
            onClick = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun UploadStatusIndicatorSuccessPreview() {
    WireTheme {
        UploadStatusIndicator(
            uploads = listOf(
                CellUploadItem(
                    id = "1",
                    conversationId = "cellName",
                    request = CellUploadRequest(
                        localPath = "/path/to/file1.txt".toPath(),
                        fileName = "file1.txt",
                        sizeBytes = 1024,
                        destinationFolderPath = "cellName/folder",
                    ),
                    state = CellUploadState.Completed,
                    nodeUuid = "node-uuid-1",
                    versionId = "version-id-1",
                ),
            ),
            onDismissAll = {},
            onClick = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun UploadStatusIndicatorFailedPreview() {
    WireTheme {
        UploadStatusIndicator(
            uploads = listOf(
                CellUploadItem(
                    id = "1",
                    conversationId = "cellName",
                    request = CellUploadRequest(
                        localPath = "/path/to/file1.txt".toPath(),
                        fileName = "file1.txt",
                        sizeBytes = 1024,
                        destinationFolderPath = "cellName/folder",
                    ),
                    state = CellUploadState.Failed,
                    nodeUuid = "node-uuid-1",
                    versionId = "version-id-1",
                ),
                CellUploadItem(
                    id = "2",
                    conversationId = "cellName",
                    request = CellUploadRequest(
                        localPath = "/path/to/file2.txt".toPath(),
                        fileName = "file2.txt",
                        sizeBytes = 1024,
                        destinationFolderPath = "cellName/folder",
                    ),
                    state = CellUploadState.Completed,
                    nodeUuid = "node-uuid-2",
                    versionId = "version-id-2",
                ),
            ),
            onDismissAll = {},
            onClick = {},
        )
    }
}
