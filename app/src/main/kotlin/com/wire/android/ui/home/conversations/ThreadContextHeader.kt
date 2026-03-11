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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.QuotedMessage
import com.wire.android.ui.home.conversations.messages.QuotedMessageStyle
import com.wire.android.ui.home.conversations.messages.QuotedStyle
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.model.MessageEditStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.mapToQuotedContent
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId

@Composable
internal fun ThreadContextHeader(
    conversationId: ConversationId,
    conversationName: String,
    rootMessage: UIMessage.Regular?,
    onOpenParentConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootPreview = remember(rootMessage) { rootMessage?.toThreadRootQuotedMessage() }
    val openParentDescription = stringResource(R.string.thread_open_in_conversation)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing12x),
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_thread),
                    color = colorsScheme().secondaryText,
                    style = MaterialTheme.wireTypography.label02,
                )
                Text(
                    text = conversationName,
                    style = MaterialTheme.wireTypography.body04,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = openParentDescription,
                color = MaterialTheme.wireColorScheme.primary,
                style = MaterialTheme.wireTypography.label03,
                modifier = Modifier.clickable(onClick = onOpenParentConversation),
            )
        }

        if (rootPreview != null) {
            QuotedMessage(
                conversationId = conversationId,
                messageData = rootPreview,
                clickable = Clickable(
                    onClickDescription = openParentDescription,
                    onClick = onOpenParentConversation,
                ),
                style = QuotedMessageStyle(
                    quotedStyle = QuotedStyle.PREVIEW,
                    messageStyle = MessageStyle.NORMAL,
                    selfAccent = Accent.Unknown,
                    senderAccent = rootPreview.senderAccent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = stringResource(R.string.thread_root_fallback_label),
                color = colorsScheme().secondaryText,
                style = MaterialTheme.wireTypography.body04,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.wireColorScheme.surfaceVariant,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius),
                    )
                    .clickable(onClick = onOpenParentConversation)
                    .padding(horizontal = dimensions().spacing12x, vertical = dimensions().spacing10x),
            )
        }
    }
}

private fun UIMessage.Regular.toThreadRootQuotedMessage(): UIQuotedMessage.UIQuotedData? {
    val senderId = header.userId ?: return null
    val quotedContent = when {
        isDeleted -> UIQuotedMessage.UIQuotedData.Deleted
        else -> mapToQuotedContent() ?: return null
    }

    return UIQuotedMessage.UIQuotedData(
        messageId = header.messageId,
        senderId = senderId,
        senderName = header.username,
        senderAccent = header.accent,
        originalMessageDateDescription = UIText.DynamicString(header.messageTime.formattedDate),
        editedTimeDescription = when (val editStatus = header.messageStatus.editStatus) {
            is MessageEditStatus.Edited -> UIText.DynamicString(editStatus.formattedEditTimeStamp)
            MessageEditStatus.NonEdited -> null
        },
        quotedContent = quotedContent,
    )
}
