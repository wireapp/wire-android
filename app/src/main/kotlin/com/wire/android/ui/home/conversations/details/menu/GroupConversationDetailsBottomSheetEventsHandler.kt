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

package com.wire.android.ui.home.conversations.details.menu

import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.util.DateTimeUtil

@Suppress("TooManyFunctions")
interface GroupConversationDetailsBottomSheetEventsHandler {
    fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus)
    fun updateConversationArchiveStatus(dialogState: DialogState, timestamp: Long = DateTimeUtil.currentInstant().toEpochMilliseconds())
    fun onClearConversationContent(dialogState: DialogState)

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : GroupConversationDetailsBottomSheetEventsHandler {
            override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {}
            override fun updateConversationArchiveStatus(dialogState: DialogState, timestamp: Long) {}
            override fun onClearConversationContent(dialogState: DialogState) {}
        }
    }
}
