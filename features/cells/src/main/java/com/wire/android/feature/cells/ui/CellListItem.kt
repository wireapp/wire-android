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
package com.wire.android.feature.cells.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.icon
import com.wire.android.feature.cells.domain.model.previewSupported
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.chip.WireDisplayChipWithOverFlow
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import com.wire.android.ui.common.R as commonR

@Composable
internal fun CellListItem(
    cell: CellNodeUi,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showReadyState by remember { mutableStateOf(false) }
    val cellState = rememberUpdatedState(cell)

    LaunchedEffect(cell.uuid) {
        snapshotFlow { cellState.value.openLoadState is OpenLoadState.Ready }
            .filter { it }
            .collect {
                showReadyState = true
                delay(3_000L)
                showReadyState = false
            }
    }

    Row(
        modifier = modifier
            .height(dimensions().spacing64x)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CellItemIcon(cell = cell, showReadyState = showReadyState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing2x)
        ) {
            Text(
                text = cell.name ?: "",
                style = typography().title02,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                CellItemSubtitle(cell = cell, showReadyState = showReadyState)
            }
        }

        Icon(
            painter = painterResource(commonR.drawable.ic_more_vert),
            contentDescription = null,
            modifier = Modifier
                .padding(end = dimensions().spacing16x)
                .clickable(
                    onClick = { onMenuClick() },
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = false,
                        radius = dimensions().spacing24x,
                        color = Color.Transparent
                    )
                )
                .then(Modifier.size(dimensions().spacing24x))
        )
    }
}

@Composable
private fun CellItemIcon(cell: CellNodeUi, showReadyState: Boolean) {
    val iconState = when {
        cell.isOpenLoading -> CellIconState.Loading(cell.openLoadProgress)
        cell.downloadProgress != null -> CellIconState.Downloading(cell.downloadProgress)
        showReadyState -> CellIconState.Ready
        cell is CellNodeUi.File -> CellIconState.FileIcon(cell)
        else -> CellIconState.FolderIcon(cell as CellNodeUi.Folder)
    }

    AnimatedContent(
        targetState = iconState,
        contentKey = { it::class.simpleName },
        transitionSpec = {
            (scaleIn(initialScale = 0.72f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.72f) + fadeOut())
        },
        label = "cell_icon_transition",
    ) { state ->
        when (state) {
            is CellIconState.Loading -> LoadingIconPreview(progress = state.progress)
            is CellIconState.Downloading -> LoadingIconPreview(progress = state.progress)
            is CellIconState.Ready -> ReadyIconPreview()
            is CellIconState.FileIcon -> FileIconPreview(state.cell)
            is CellIconState.FolderIcon -> FolderIconPreview(state.cell)
        }
    }
}

@Composable
private fun CellItemSubtitle(cell: CellNodeUi, showReadyState: Boolean) {
    when {
        cell.openLoadState is OpenLoadState.Loading -> Text(
            text = stringResource(R.string.tap_to_cancel_loading),
            textAlign = TextAlign.Left,
            overflow = TextOverflow.Ellipsis,
            style = typography().label04,
            color = colorsScheme().secondaryText,
            maxLines = 1,
        )
        cell.openLoadState is OpenLoadState.Error -> Text(
            text = stringResource(R.string.unable_to_load_retry),
            textAlign = TextAlign.Left,
            overflow = TextOverflow.Ellipsis,
            style = typography().label04,
            color = colorsScheme().error,
            maxLines = 1,
        )

        cell.downloadProgress != null ->
            Text(
                text = stringResource(R.string.tap_to_cancel_download),
                textAlign = TextAlign.Left,
                overflow = TextOverflow.Ellipsis,
                style = typography().label04,
                color = colorsScheme().secondaryText,
                maxLines = 1,
            )
        showReadyState -> Text(
            text = stringResource(R.string.ready_to_open),
            textAlign = TextAlign.Left,
            overflow = TextOverflow.Ellipsis,
            style = typography().label04,
            color = colorsScheme().primary,
            maxLines = 1,
        )
        else -> {
            if (cell.isAvailableOffline) {
                Icon(
                    modifier = Modifier
                        .padding(end = dimensions().spacing6x),
                    painter = painterResource(R.drawable.ic_downloaded),
                    contentDescription = null,
                    tint = colorsScheme().secondaryText,
                )
            }

            if (cell.tags.isNotEmpty()) {
                WireDisplayChipWithOverFlow(
                    label = cell.tags.first(),
                    chipsCount = cell.tags.size - 1,
                    modifier = Modifier.padding(end = dimensions().spacing4x)
                )
            }
            cell.subtitle()?.let {
                Text(
                    text = it,
                    textAlign = TextAlign.Left,
                    overflow = TextOverflow.Ellipsis,
                    style = typography().label04,
                    color = colorsScheme().secondaryText,
                    maxLines = 1,
                )
            }
        }
    }
}

private sealed class CellIconState {
    data class Loading(val progress: Float?) : CellIconState()
    data class Downloading(val progress: Float?) : CellIconState()
    data object Ready : CellIconState()
    data class FileIcon(val cell: CellNodeUi.File) : CellIconState()
    data class FolderIcon(val cell: CellNodeUi.Folder) : CellIconState()
}

@Composable
internal fun LoadingIconPreview(progress: Float?) {
    val modifier = Modifier.size(dimensions().spacing32x)
    val color = colorsScheme().primary
    val trackColor = colorsScheme().primaryVariant
    val strokeWidth = dimensions().spacing2x
    Box(
        modifier = Modifier.size(dimensions().spacing56x),
        contentAlignment = Alignment.Center
    ) {
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = modifier,
                color = color,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                strokeCap = StrokeCap.Round,
            )
        } else {
            CircularProgressIndicator(
                modifier = modifier,
                color = color,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun ReadyIconPreview() {
    Box(
        modifier = Modifier.size(dimensions().spacing56x),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(dimensions().spacing32x),
            painter = painterResource(commonR.drawable.ic_check_circle),
            contentDescription = null,
            tint = colorsScheme().primary,
        )
    }
}

@Composable
internal fun FolderIconPreview(cell: CellNodeUi.Folder) {
    Box(
        modifier = Modifier
            .size(dimensions().spacing56x),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(dimensions().spacing32x),
            painter = painterResource(R.drawable.ic_folder_item),
            contentDescription = null,
        )
        cell.publicLinkId?.let {
            PublicLinkIcon(
                offsetX = dimensions().spacing12x,
                offsetY = dimensions().spacing12x
            )
        }
    }
}

@Composable
internal fun FileIconPreview(cell: CellNodeUi.File) {
    Box(
        modifier = Modifier
            .size(dimensions().spacing56x),
        contentAlignment = Alignment.Center
    ) {
        if (cell.previewUrl != null && cell.assetType.previewSupported()) {

            val builder = ImageRequest.Builder(LocalContext.current)
                .diskCacheKey(cell.contentHash)
                .memoryCacheKey(cell.contentHash)
                .data(cell.previewUrl)
                .crossfade(true)

            AsyncImage(
                modifier = Modifier
                    .size(dimensions().spacing40x)
                    .clip(RoundedCornerShape(dimensions().spacing4x))
                    .background(
                        color = colorsScheme().outline,
                        shape = RoundedCornerShape(dimensions().spacing4x)
                    )
                    .border(
                        width = dimensions().spacing1x,
                        color = colorsScheme().outline,
                        shape = RoundedCornerShape(dimensions().spacing4x)
                    ),
                contentScale = ContentScale.Crop,
                model = builder.build(),
                contentDescription = null,
            )
            cell.publicLinkId?.let {
                PublicLinkIcon(
                    offsetX = dimensions().spacing16x,
                    offsetY = dimensions().spacing16x
                )
            }
        } else {
            Image(
                modifier = Modifier.size(dimensions().spacing32x),
                painter = painterResource(cell.assetType.icon()),
                contentDescription = null,
            )
            cell.publicLinkId?.let {
                PublicLinkIcon()
            }
        }
    }
}

@Composable
private fun PublicLinkIcon(
    offsetX: Dp = dimensions().spacing12x,
    offsetY: Dp = dimensions().spacing12x,
) {
    Icon(
        modifier = Modifier
            .size(dimensions().spacing20x)
            .offset(
                x = offsetX,
                y = offsetY
            )
            .background(
                color = colorsScheme().surfaceVariant,
                shape = CircleShape
            )
            .border(
                width = dimensions().spacing1x,
                color = colorsScheme().outline,
                shape = CircleShape
            )
            .padding(dimensions().spacing4x),
        painter = painterResource(commonR.drawable.ic_link_indicator),
        contentDescription = null,
    )
}

@Composable
private fun CellNodeUi.subtitle() =
    when {
        userName != null && conversationName != null -> {
            stringResource(R.string.file_subtitle, userName!!, conversationName!!)
        }

        userName != null && modifiedTime != null -> {
            stringResource(R.string.file_subtitle_modified, modifiedTime!!, userName!!)
        }

        userName != null -> userName
        conversationName != null -> conversationName
        modifiedTime != null -> modifiedTime
        else -> null
    }

@PreviewMultipleThemes
@Composable
private fun PreviewCellListItem() {
    WireTheme {
        CellListItem(
            cell = CellNodeUi.File(
                uuid = "",
                name = "file name",
                assetType = AttachmentFileType.IMAGE,
                size = 123214,
                localPath = null,
                mimeType = "image/jpg",
                publicLinkId = "",
                userName = "Test User",
                userHandle = "userId",
                ownerUserId = "userId",
                conversationName = "Test Conversation",
                modifiedTime = null,
                remotePath = null,
                contentHash = null,
                contentUrl = null,
                previewUrl = null
            ),
            onMenuClick = {},
        )
    }
}
