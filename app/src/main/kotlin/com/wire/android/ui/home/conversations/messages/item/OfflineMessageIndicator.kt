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

package com.wire.android.ui.home.conversations.messages.item

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.datetime.Clock

internal fun PagingData<UIMessage>.withOfflineIndicator(
    conversationId: ConversationId,
    isOffline: Boolean,
): PagingData<UIMessage> {
    if (!isOffline) return this

    return insertSeparators { before, after ->
        val keySuffix = when {
            before == null && after == null -> "empty"
            before == null && after?.isPending == false -> "before:${after.header.messageId}"
            before?.isPending == true && after?.isPending != true -> "after:${before.header.messageId}"
            else -> null
        }

        if (keySuffix != null) {
            offlineMessage(conversationId, keySuffix)
        } else {
            null
        }
    }
}

internal fun offlineMessage(conversationId: ConversationId, keySuffix: String): UIMessage.System =
    UIMessage.System(
        conversationId = conversationId,
        source = MessageSource.Self,
        messageContent = UIMessageContent.SystemMessage.Offline,
        header = MessageHeader(
            username = UIText.DynamicString(""),
            membership = Membership.None,
            showLegalHoldIndicator = false,
            messageTime = MessageTime(Clock.System.now()),
            messageStatus = MessageStatus(
                flowStatus = MessageFlowStatus.Sent,
                expirationStatus = ExpirationStatus.NotExpirable,
            ),
            messageId = "offline-message:$conversationId:$keySuffix",
            userId = null,
            connectionState = null,
            isSenderDeleted = false,
            isSenderUnavailable = false,
        ),
    )
