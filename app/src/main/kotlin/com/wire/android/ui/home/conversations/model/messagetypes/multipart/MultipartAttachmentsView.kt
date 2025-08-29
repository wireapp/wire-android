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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil.decode.Decoder
import coil.request.ImageRequest
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.toUiModel
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.grid.AssetGridPreview
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.standalone.AssetPreview
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.isFailed
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageAttachment

/**
 * Displays a list of message attachments as a grid or a single attachment card.
 * Uses [MultipartAttachmentsViewModel] to handle preview image loading and interactions handling.
 */
@Composable
fun MultipartAttachmentsView(
    conversationId: ConversationId,
    attachments: List<MessageAttachment>,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    viewModel: MultipartAttachmentsViewModel = hiltViewModel<MultipartAttachmentsViewModel>(key = conversationId.value),
) {

    // TODO I found out that empty attachments list is not handled here and it shows empty message with no information
    if (attachments.size == 1) {
        attachments.first().toUiModel().let {
            AssetPreview(
                item = it,
                messageStyle = messageStyle,
                onClick = { viewModel.onClick(it) },
            )
        }
    } else {
        val groups = viewModel.mapAttachments(attachments)

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
        ) {
            groups.forEach { group ->
                when (group) {
                    is MultipartAttachmentsViewModel.MultipartAttachmentGroup.Media ->
                        if (group.attachments.size == 1) {
                            AssetPreview(
                                item = group.attachments.first(),
                                messageStyle = messageStyle,
                                onClick = { viewModel.onClick(group.attachments.first()) },
                            )
                        } else {
                            AttachmentsGrid(
                                attachments = group.attachments,
                                messageStyle = messageStyle,
                                onClick = { viewModel.onClick(it) },
                            )
                        }

                    is MultipartAttachmentsViewModel.MultipartAttachmentGroup.Files ->
                        AttachmentsList(
                            attachments = group.attachments,
                            messageStyle = messageStyle,
                            onClick = { viewModel.onClick(it) }
                        )
                }
            }
        }
    }
    LaunchedEffect(attachments) {
        attachments.onEach { viewModel.refreshAssetState(it.toUiModel()) }
    }
}

@Composable
private fun AttachmentsList(
    attachments: List<MultipartAttachmentUi>,
    messageStyle: MessageStyle,
    onClick: (MultipartAttachmentUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
    ) {
        attachments.forEach {
            AssetPreview(
                item = it,
                messageStyle = messageStyle,
                onClick = { onClick(it) },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AttachmentsGrid(
    attachments: List<MultipartAttachmentUi>,
    messageStyle: MessageStyle,
    onClick: (MultipartAttachmentUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier.heightIn(max = dimensions().attachmentGridMaxHeight),
        columns = GridCells.Fixed(attachmentColumnCount(LocalConfiguration.current)),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
    ) {
        items(
            items = attachments,
            key = { it.uuid },
        ) { item ->
            AssetGridPreview(
                item = item,
                messageStyle = messageStyle,
                onClick = { onClick(item) },
            )
        }
    }
}

@Suppress("MagicNumber")
private fun attachmentColumnCount(configuration: Configuration) =
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> 4
        else -> 2
    }

internal fun MultipartAttachmentUi.previewAvailable() = localPath != null || previewUrl != null
internal fun MultipartAttachmentUi.getPreview() = localPath ?: previewUrl

@Composable
internal fun MultipartAttachmentUi.previewImageModel(decoderFactory: Decoder.Factory? = null): Any? =
    if (previewAvailable()) {
        val builder = ImageRequest.Builder(LocalContext.current)
            .diskCacheKey(contentHash)
            .memoryCacheKey(contentHash)
            .data(getPreview())
            .crossfade(true)

        if (localPath != null && decoderFactory != null) {
            builder.decoderFactory(decoderFactory)
        }
        builder.build()
    } else {
        null
    }

@Composable
internal fun transferProgressColor(status: AssetTransferStatus) = if (status.isFailed()) colorsScheme().error else colorsScheme().primary
