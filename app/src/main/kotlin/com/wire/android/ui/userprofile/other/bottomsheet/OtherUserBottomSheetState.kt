package com.wire.android.ui.userprofile.other.bottomsheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.userprofile.other.OtherUserProfileGroupState

class OtherUserBottomSheetState {

    private var conversationSheetContent: ConversationSheetContent? by mutableStateOf(null)
    private var groupState: OtherUserProfileGroupState? by mutableStateOf(null)
    private var flag: OtherUserBottomSheetContentFlag = OtherUserBottomSheetContentFlag.NONE
    var bottomSheetContentState: BottomSheetContent? by mutableStateOf(null)

    fun toConversation() {
        conversationSheetContent?.let { bottomSheetContentState = BottomSheetContent.Conversation(it) }
        flag = OtherUserBottomSheetContentFlag.CONVERSATION
    }

    fun setContents(conversationSheetContent: ConversationSheetContent?, groupState: OtherUserProfileGroupState?) {
        this.conversationSheetContent = conversationSheetContent
        this.groupState = groupState
        updateBottomSheetContentIfNeeded()
    }

    private fun updateBottomSheetContentIfNeeded() {
        when (flag) {
            OtherUserBottomSheetContentFlag.CONVERSATION -> toConversation()
            OtherUserBottomSheetContentFlag.CHANGE_ROLE -> toChangeRole()
            else -> {}
        }
    }

    fun toChangeRole() {
        groupState?.let { bottomSheetContentState = BottomSheetContent.ChangeRole(it) }
        flag = OtherUserBottomSheetContentFlag.CHANGE_ROLE
    }

    fun clearBottomSheetState() {
        bottomSheetContentState = null
        flag = OtherUserBottomSheetContentFlag.NONE
    }

}

enum class OtherUserBottomSheetContentFlag {
    NONE, CHANGE_ROLE, CONVERSATION
}

sealed class BottomSheetContent {
    data class Conversation(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class ChangeRole(val groupState: OtherUserProfileGroupState) : BottomSheetContent()
}
