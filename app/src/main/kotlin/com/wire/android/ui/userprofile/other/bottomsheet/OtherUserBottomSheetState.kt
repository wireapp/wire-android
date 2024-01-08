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

package com.wire.android.ui.userprofile.other.bottomsheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.userprofile.other.OtherUserProfileGroupState

class OtherUserBottomSheetState {

    private var conversationSheetContent: ConversationSheetContent? by mutableStateOf(null)
    private var groupState: OtherUserProfileGroupState? by mutableStateOf(null)
    private var contentFlag: OtherUserBottomSheetContentFlag = OtherUserBottomSheetContentFlag.NONE
    var bottomSheetContentState: BottomSheetContent? by mutableStateOf(null)

    fun toConversation() {
        conversationSheetContent?.let { bottomSheetContentState = BottomSheetContent.Conversation(it) }
        contentFlag = OtherUserBottomSheetContentFlag.CONVERSATION
    }

    fun setContents(conversationSheetContent: ConversationSheetContent?, groupState: OtherUserProfileGroupState?) {
        this.conversationSheetContent = conversationSheetContent
        this.groupState = groupState
        updateBottomSheetContentIfNeeded()
    }

    private fun updateBottomSheetContentIfNeeded() {
        when (contentFlag) {
            OtherUserBottomSheetContentFlag.CONVERSATION -> toConversation()
            OtherUserBottomSheetContentFlag.CHANGE_ROLE -> toChangeRole()
            OtherUserBottomSheetContentFlag.NONE -> {}
        }
    }

    fun toChangeRole() {
        groupState?.let { bottomSheetContentState = BottomSheetContent.ChangeRole(it) }
        contentFlag = OtherUserBottomSheetContentFlag.CHANGE_ROLE
    }

    fun clearBottomSheetState() {
        bottomSheetContentState = null
        contentFlag = OtherUserBottomSheetContentFlag.NONE
    }

}

enum class OtherUserBottomSheetContentFlag {
    NONE, CHANGE_ROLE, CONVERSATION
}

sealed class BottomSheetContent {
    data class Conversation(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class ChangeRole(val groupState: OtherUserProfileGroupState) : BottomSheetContent()
}
