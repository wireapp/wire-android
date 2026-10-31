/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.runtime.MutableState
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.draft.MessageDraft

data class MessageComposition(
    val conversationId: ConversationId,
    val draftText: String = String.EMPTY,
    val editMessageId: String? = null,
    val quotedMessage: UIQuotedMessage.UIQuotedData? = null,
    val quotedMessageId: String? = null,
    val selectedMentions: List<UIMention> = emptyList(),
    val isMultipart: Boolean = false,
) {
    /** Targets follow untouched UTF-16 ranges, never another occurrence of the same label. */
    @OptIn(ExperimentalFoundationApi::class)
    fun getSelectedMentions(buffer: TextFieldBuffer): List<UIMention> = selectedMentions.mapNotNull { mention ->
        var shift = 0
        for (index in 0 until buffer.changes.changeCount) {
            val original = buffer.changes.getOriginalRange(index)
            when {
                original.max <= mention.start -> {
                    shift += buffer.changes.getRange(index).length - original.length
                }
                original.min < mention.start + mention.length -> return@mapNotNull null
            }
        }
        val shifted = mention.copy(start = mention.start + shift)
        val hasValidRange = shifted.start >= 0 && shifted.length > 0 && shifted.start <= buffer.length - shifted.length
        if (!hasValidRange) return@mapNotNull null
        shifted.takeIf {
            buffer.asCharSequence().subSequence(it.start, it.start + it.length).toString() == it.handler
        }
    }

    fun toMessageBundle(
        conversationId: ConversationId,
        messageText: String,
        attachments: List<AttachmentDraftUi>
    ): ComposableMessageBundle {
        return if (editMessageId != null) {
            if (isMultipart) {
                ComposableMessageBundle.EditMultipartMessageBundle(
                    conversationId = conversationId,
                    originalMessageId = editMessageId,
                    newContent = messageText,
                    newMentions = selectedMentions
                )
            } else {
                ComposableMessageBundle.EditMessageBundle(
                    conversationId = conversationId,
                    originalMessageId = editMessageId,
                    newContent = messageText,
                    newMentions = selectedMentions
                )
            }
        } else {
            if (attachments.isEmpty()) {
                ComposableMessageBundle.SendTextMessageBundle(
                    conversationId = conversationId,
                    message = messageText,
                    mentions = selectedMentions,
                    quotedMessageId = quotedMessageId
                )
            } else {
                ComposableMessageBundle.SendMultipartMessageBundle(
                    conversationId = conversationId,
                    message = messageText,
                    mentions = selectedMentions,
                    quotedMessageId = quotedMessageId
                )
            }
        }
    }
}

fun MutableState<MessageComposition>.update(block: (MessageComposition) -> MessageComposition) {
    val currentValue = value
    value = block(currentValue)
}

fun MessageComposition.toDraft(messageText: String): MessageDraft {
    return MessageDraft(
        conversationId = conversationId,
        text = messageText,
        editMessageId = editMessageId,
        quotedMessageId = quotedMessageId,
        selectedMentionList = selectedMentions.map { it.intoMessageMention() },
        isMultipartEdit = isMultipart,
    )
}
