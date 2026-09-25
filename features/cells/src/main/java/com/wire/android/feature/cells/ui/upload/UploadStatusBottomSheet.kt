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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.icon
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.typography
import com.wire.android.util.FileSizeFormatter
import com.wire.kalium.cells.domain.CellUploadItem
import com.wire.kalium.cells.domain.CellUploadState
import com.wire.kalium.logic.util.fileExtension
import com.wire.android.ui.common.R as commonR

@Composable
internal fun UploadStatusBottomSheetContent(
    uploads: List<CellUploadItem>,
    onCollapse: () -> Unit,
    onCancel: (String) -> Unit,
    onCancelAll: () -> Unit,
    onRetry: (String) -> Unit,
    onRetryAllFailed: () -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val failedCount = uploads.count { it.state is CellUploadState.Failed }

    val activeCount = uploads.count { it.state is CellUploadState.Queued || it.state is CellUploadState.Uploading }

    Column(modifier = modifier.fillMaxWidth()) {
        UploadStatusHeader(
            uploads = uploads,
            failedCount = failedCount,
            activeCount = activeCount,
            onCollapse = onCollapse,
            onRetryAllFailed = onRetryAllFailed,
            onCancelAll = onCancelAll,
        )
        LazyColumn {
            items(uploads.asReversed(), key = { it.id }) { item ->
                UploadStatusRow(
                    item = item,
                    onCancel = { onCancel(item.id) },
                    onRetry = { onRetry(item.id) },
                    onDismiss = { onDismiss(item.id) },
                )
                WireDivider(modifier = Modifier.fillMaxWidth(), color = colorsScheme().outline)
            }
        }
    }
}

@Composable
private fun UploadStatusHeader(
    uploads: List<CellUploadItem>,
    failedCount: Int,
    activeCount: Int,
    onCollapse: () -> Unit,
    onRetryAllFailed: () -> Unit,
    onCancelAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing12x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.size(dimensions().spacing32x),
        ) {
            Icon(
                painter = painterResource(commonR.drawable.ic_keyboard_arrow_down),
                contentDescription = stringResource(NavigationIconType.Collapse.contentDescription),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensions().spacing12x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing2x),
        ) {
            Text(
                text = uploadStatusTitle(uploads),
                style = typography().title02,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (failedCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.cells_upload_status_subtitle_failed_count, failedCount, failedCount),
                    style = typography().label04,
                    color = colorsScheme().secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)) {
            if (activeCount > 0) {
                WireSecondaryButton(
                    modifier = Modifier.height(dimensions().spacing32x),
                    text = stringResource(R.string.cells_upload_status_cancel_all),
                    onClick = onCancelAll,
                    fillMaxWidth = false,
                )
            }
            if (failedCount > 0) {
                WireSecondaryButton(
                    modifier = Modifier.height(dimensions().spacing32x),
                    text = stringResource(R.string.cells_upload_status_retry_failed),
                    onClick = onRetryAllFailed,
                    fillMaxWidth = false,
                )
            }
        }
    }
}

/**
 * "Uploading N files" while nothing has finished yet, "Upload complete" once every file succeeded,
 * and "X of Y uploaded" otherwise — while some are still running/queued after others finished, or
 * once the batch ended with a failure.
 */
@Composable
internal fun uploadStatusTitle(uploads: List<CellUploadItem>): String {
    val total = uploads.size
    val completed = uploads.count { it.state is CellUploadState.Completed }
    val finished = uploads.count {
        it.state is CellUploadState.Completed || it.state is CellUploadState.Failed || it.state is CellUploadState.Cancelled
    }
    return when {
        total == 0 || (finished == total && finished == completed) ->
            stringResource(R.string.cells_upload_status_title_complete)

        finished == 0 -> pluralStringResource(R.plurals.cells_upload_status_title_uploading, total, total)
        else -> stringResource(R.string.cells_upload_status_title_partial, completed, total)
    }
}

@Composable
private fun UploadStatusRow(
    item: CellUploadItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UploadStatusRowIcon(state = item.state, fileName = item.fileName)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensions().spacing12x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing2x),
        ) {
            Text(
                text = item.fileName,
                style = typography().title02,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            UploadStatusRowSubtitle(item = item)
        }

        UploadStatusRowActions(state = item.state, onCancel = onCancel, onRetry = onRetry, onDismiss = onDismiss)
    }
}

@Composable
private fun UploadStatusRowIcon(state: CellUploadState, fileName: String) {
    Box(
        modifier = Modifier.size(dimensions().spacing40x),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is CellUploadState.Uploading -> {
                Box(
                    modifier = Modifier.size(dimensions().spacing56x),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.size(dimensions().spacing32x),
                        color = colorsScheme().primary,
                        trackColor = colorsScheme().primaryVariant,
                        strokeWidth = dimensions().spacing2x,
                        strokeCap = StrokeCap.Round,
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_up),
                        contentDescription = null,
                        tint = colorsScheme().primary,
                        modifier = Modifier.size(dimensions().spacing16x)
                    )
                }
            }

            CellUploadState.Failed -> Icon(
                painter = painterResource(commonR.drawable.ic_warning_amber),
                contentDescription = null,
                tint = colorsScheme().error,
                modifier = Modifier
                    .size(dimensions().spacing32x)
                    .background(color = colorsScheme().errorVariant, shape = CircleShape)
                    .padding(dimensions().spacing6x),
            )

            CellUploadState.Queued, CellUploadState.Completed, CellUploadState.Cancelled -> {
                val fileType = remember(fileName) {
                    fileName.fileExtension()?.let(AttachmentFileType::fromExtension) ?: AttachmentFileType.OTHER
                }
                Image(
                    painter = painterResource(fileType.icon()),
                    contentDescription = null,
                    modifier = Modifier.size(dimensions().spacing28x),
                )
            }
        }
    }
}

@Composable
private fun UploadStatusRowSubtitle(item: CellUploadItem) {
    when (item.state) {
        CellUploadState.Queued -> UploadStatusSubtitleText(
            text = stringResource(R.string.cells_upload_status_subtitle_queued),
            color = colorsScheme().secondaryText,
        )

        is CellUploadState.Uploading -> UploadStatusSubtitleText(
            text = stringResource(R.string.cells_upload_status_subtitle_uploading),
            color = colorsScheme().secondaryText,
        )

        CellUploadState.Completed -> {
            val context = LocalContext.current
            val sizeText = remember(item.sizeBytes) { FileSizeFormatter(context).formatSize(item.sizeBytes) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
                Icon(
                    painter = painterResource(commonR.drawable.ic_check_tick),
                    contentDescription = null,
                    tint = colorsScheme().positive,
                    modifier = Modifier.size(dimensions().spacing12x),
                )
                UploadStatusSubtitleText(
                    text = stringResource(R.string.cells_upload_status_subtitle_completed_with_size, sizeText),
                    color = colorsScheme().secondaryText,
                )
            }
        }

        CellUploadState.Failed -> UploadStatusSubtitleText(
            text = stringResource(R.string.cells_upload_status_subtitle_failed),
            color = colorsScheme().error,
        )

        CellUploadState.Cancelled -> UploadStatusSubtitleText(
            text = stringResource(R.string.cells_upload_status_subtitle_cancelled),
            color = colorsScheme().secondaryText,
        )
    }
}

@Composable
private fun UploadStatusSubtitleText(text: String, color: Color) {
    Text(
        text = text,
        style = typography().label04,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun UploadStatusRowActions(
    state: CellUploadState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        CellUploadState.Queued, is CellUploadState.Uploading -> IconButton(
            onClick = onCancel,
            modifier = Modifier.size(dimensions().spacing32x),
        ) {
            Icon(
                painter = painterResource(commonR.drawable.ic_close),
                contentDescription = stringResource(R.string.content_description_cancel_upload),
            )
        }

        CellUploadState.Failed, CellUploadState.Cancelled -> Row {
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(dimensions().spacing32x),
            ) {
                Icon(
                    painter = painterResource(commonR.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.content_description_retry_upload),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(dimensions().spacing32x),
            ) {
                Icon(
                    painter = painterResource(commonR.drawable.ic_close),
                    contentDescription = stringResource(R.string.content_description_dismiss_upload),
                )
            }
        }

        CellUploadState.Completed -> Unit
    }
}
