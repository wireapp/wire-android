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
package com.wire.android.ui.home.conversations.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.wire.android.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.AttachmentFileType.VIDEO
import com.wire.android.feature.cells.domain.model.icon
import com.wire.android.model.Clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.home.conversations.model.UIMultipartQuotedContent
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.util.fileExtension

@Composable
fun QuotedMultipartMessage(
    conversationId: ConversationId,
    quotedMessageId: String,
    senderName: UIText,
    originalDateTimeText: UIText,
    text: String?,
    style: QuotedMessageStyle,
    accent: Accent,
    clickable: Clickable?,
    modifier: Modifier = Modifier,
    viewModel: QuotedMultipartMessageViewModel = hiltViewModel(key = conversationId.toString()),
    startContent: @Composable () -> Unit = {}
) {

    var quotedMultipartMessage by remember { mutableStateOf(UIQuotedMultipartMessage()) }

    LaunchedEffect(quotedMessageId) {
        viewModel.observeMultipartMessage(conversationId, quotedMessageId)
            .collect {
                quotedMultipartMessage = it
            }
    }

    QuotedMultipartMessageContent(
        quotedMultipartMessage = quotedMultipartMessage,
        senderName = senderName,
        originalDateTimeText = originalDateTimeText,
        text = text,
        style = style,
        accent = accent,
        clickable = clickable,
        modifier = modifier,
        startContent = startContent
    )
}

@Composable
private fun MediaAttachmentThumbnail(attachment: UIMultipartQuotedContent) {
    if (attachment.assetAvailable) {
        Box(
            modifier = Modifier
                .width(dimensions().spacing40x)
                .height(dimensions().spacing40x)
                .border(
                    width = dimensions().spacing1x,
                    color = MaterialTheme.wireColorScheme.outline,
                    shape = RoundedCornerShape(dimensions().spacing8x)
                )
                .clip(RoundedCornerShape(dimensions().spacing8x)),
        ) {
            AsyncImage(
                model = attachment.imageModel(),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )

            if (AttachmentFileType.fromMimeType(attachment.mimeType) == VIDEO) {
                Image(
                    modifier = Modifier
                        .size(dimensions().spacing24x)
                        .align(Alignment.Center),
                    painter = painterResource(id = R.drawable.ic_play_circle_filled),
                    contentDescription = null,
                )
            }
        }
    } else {
        Image(
            modifier = Modifier.size(dimensions().spacing24x),
            painter = painterResource(id = R.drawable.ic_file_not_available),
            contentDescription = null,
        )
    }
}

@Composable
private fun MultipleAttachmentsLabel(style: QuotedMessageStyle, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_multiple_files),
            contentDescription = null,
            tint = style.messageStyle.textColor(),
        )
        Text(
            text = pluralStringResource(R.plurals.reply_multiple_files, count, count),
            color = style.messageStyle.textColor(),
        )
    }
}

@Composable
private fun FileIconAndNameRow(style: QuotedMessageStyle, file: UIMultipartQuotedContent) {

    val name = file.name
    val attachmentFileType = name.fileExtension()?.let { AttachmentFileType.fromExtension(it) } ?: AttachmentFileType.OTHER

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)
    ) {
        Image(
            modifier = Modifier.size(dimensions().spacing16x),
            painter = if (file.assetAvailable) {
                painterResource(id = attachmentFileType.icon())
            } else {
                painterResource(id = R.drawable.ic_file_not_available)
            },
            contentDescription = null,
        )
        Text(
            text = if (file.assetAvailable) name else stringResource(R.string.asset_message_failed_download_text),
            color = style.messageStyle.textColor(),
        )
    }
}

@Composable
fun QuotedMultipartMessageContent(
    quotedMultipartMessage: UIQuotedMultipartMessage,
    senderName: UIText,
    originalDateTimeText: UIText,
    text: String?,
    style: QuotedMessageStyle,
    accent: Accent,
    clickable: Clickable?,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {
    QuotedMessageContent(
        senderName = senderName.asString(),
        style = style,
        modifier = modifier,
        centerContent = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    dimensions().spacing4x,
                    Alignment.CenterVertically
                ),
            ) {

                // Message text or file name
                if (text?.isNotEmpty() == true) {
                    MainMarkdownText(
                        text = text,
                        messageStyle = style.messageStyle,
                        accent = accent,
                    )
                } else {
                    quotedMultipartMessage.mediaAttachment?.let { mediaAttachment ->
                        if (mediaAttachment.assetAvailable) {
                            MainContentText(
                                text = mediaAttachment.name,
                                messageStyle = style.messageStyle
                            )
                        } else {
                            MainContentText(
                                text = stringResource(R.string.asset_message_failed_download_text),
                                messageStyle = style.messageStyle
                            )
                        }
                    }
                }

                if (quotedMultipartMessage.attachmentsCount > 1) {
                    MultipleAttachmentsLabel(style, quotedMultipartMessage.attachmentsCount)
                }

                quotedMultipartMessage.fileAttachment?.let { fileAttachment ->
                    FileIconAndNameRow(style, fileAttachment)
                }
            }
        },
        startContent = {
            startContent()
        },
        endContent = {
            quotedMultipartMessage.mediaAttachment?.let { mediaAttachment ->
                MediaAttachmentThumbnail(mediaAttachment)
            }
        },
        footerContent = { QuotedMessageOriginalDate(originalDateTimeText, style) },
        clickable = clickable
    )
}

@Composable
private fun UIMultipartQuotedContent.imageModel(): ImageRequest {

    val builder = ImageRequest.Builder(LocalContext.current)
        .diskCacheKey(previewUrl)
        .memoryCacheKey(previewUrl)
        .data(localPath ?: previewUrl)
        .crossfade(true)

    if (localPath != null && AttachmentFileType.fromMimeType(mimeType) == VIDEO) {
        builder.decoderFactory { result, options, _ ->
            VideoFrameDecoder(result.source, options)
        }
    }

    return builder.build()
}
