/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.details.menu

import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.util.DateTimeUtil

@Suppress("TooManyFunctions")
interface GroupConversationDetailsBottomSheetEventsHandler {
    fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus, onMessage: (UIText) -> Unit)
    fun onAddConversationToFavourites(conversationId: ConversationId? = null)
    fun onMoveConversationToFolder(conversationId: ConversationId? = null)
    fun updateConversationArchiveStatus(
        conversationId: ConversationId,
        shouldArchive: Boolean,
        timestamp: Long = DateTimeUtil.currentInstant().toEpochMilliseconds(),
        onMessage: (UIText) -> Unit
    )

    fun onClearConversationContent(dialogState: DialogState, onMessage: (UIText) -> Unit)

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : GroupConversationDetailsBottomSheetEventsHandler {
            override fun onMutingConversationStatusChange(
                conversationId: ConversationId?,
                status: MutedConversationStatus,
                onMessage: (UIText) -> Unit
            ) {
            }

            override fun onAddConversationToFavourites(conversationId: ConversationId?) {}
            override fun onMoveConversationToFolder(conversationId: ConversationId?) {}
            override fun updateConversationArchiveStatus(
                conversationId: ConversationId,
                shouldArchive: Boolean,
                timestamp: Long,
                onMessage: (UIText) -> Unit
            ) {
            }

            override fun onClearConversationContent(dialogState: DialogState, onMessage: (UIText) -> Unit) {}
        }
    }
}
