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
package com.wire.android.ui.home.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun ThreadContextHeader(
    rootMessage: UIMessage.Regular?,
    onOpenParentConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootPreviewText = threadRootPreviewText(rootMessage)
    val cardShape = RoundedCornerShape(dimensions().corner16x)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rootPreviewText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.wireTypography.body04,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = dimensions().spacing300x)
                .shadow(
                    elevation = dimensions().spacing8x,
                    shape = cardShape,
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = cardShape,
                )
                .clickable(
                    onClickLabel = stringResource(R.string.thread_open_in_conversation),
                    onClick = onOpenParentConversation,
                )
                .padding(horizontal = dimensions().spacing12x, vertical = dimensions().spacing10x),
        )
    }
}

@Composable
private fun threadRootPreviewText(rootMessage: UIMessage.Regular?): String {
    if (rootMessage == null) {
        return stringResource(R.string.thread_root_fallback_label)
    }
    if (rootMessage.isDeleted) {
        return stringResource(R.string.deleted_message_text)
    }

    return when (val content = rootMessage.messageContent) {
        is UIMessageContent.TextMessage -> content.messageBody.message.asString()
        is UIMessageContent.Multipart -> content.messageBody?.message?.asString()
            ?: stringResource(R.string.notification_shared_file)
        is UIMessageContent.AssetMessage -> content.assetName
        is UIMessageContent.VideoMessage -> content.assetName
        is UIMessageContent.RestrictedAsset -> content.assetName
        is UIMessageContent.ImageMessage -> stringResource(R.string.notification_shared_picture)
        is UIMessageContent.AudioAssetMessage -> stringResource(R.string.notification_audio_message_body)
        is UIMessageContent.Location -> content.name.ifBlank { stringResource(R.string.notification_shared_location) }
        is UIMessageContent.MissingThreadRoot -> stringResource(R.string.thread_missing_root_message)
        else -> stringResource(R.string.thread_root_fallback_label)
    }
}
