package com.wire.android.ui.userprofile.other.bottomsheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.userprofile.other.OtherUserProfileGroupState

class OtherUserBottomSheetState {

    private var conversationSheetContent: ConversationSheetContent? by mutableStateOf(null)
    private var groupState: OtherUserProfileGroupState? by mutableStateOf(null)
    var bottomSheetContentState: BottomSheetContent? by mutableStateOf(null)

    fun toConversation() = conversationSheetContent?.let { bottomSheetContentState = BottomSheetContent.Conversation(it) }

    fun setContents(conversationSheetContent: ConversationSheetContent?, groupState: OtherUserProfileGroupState?) {
        this.conversationSheetContent = conversationSheetContent
        this.groupState = groupState
        updateBottomSheetContentIfNeeded()
    }

    private fun updateBottomSheetContentIfNeeded() {
        when (bottomSheetContentState) {
            is BottomSheetContent.Conversation -> toConversation()
            is BottomSheetContent.ChangeRole -> toChangeRole()
            null -> {}
        }
    }

    fun toChangeRole() = groupState?.let { bottomSheetContentState = BottomSheetContent.ChangeRole(it) }

    fun clearBottomSheetState() {
        bottomSheetContentState = null
    }

}

sealed class BottomSheetContent {
    data class Conversation(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class ChangeRole(val groupState: OtherUserProfileGroupState) : BottomSheetContent()
}
