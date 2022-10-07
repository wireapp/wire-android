package com.wire.android.ui.userprofile.other.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.userprofile.other.OtherUserProfileGroupState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class OtherUserBottomSheetState(
    private val groupState: OtherUserProfileGroupState? = null,
    conversationSheetContent: ConversationSheetContent? = null,
) {

    var conversationSheetContent: ConversationSheetContent? by mutableStateOf(conversationSheetContent)
    var bottomSheetContentState: BottomSheetContent? by mutableStateOf(null)

    fun toConversation() = conversationSheetContent?.let { bottomSheetContentState = BottomSheetContent.Conversation(it) }

    fun muteConversation(mutedConversationStatus: MutedConversationStatus) {
        conversationSheetContent = conversationSheetContent?.copy(mutingConversationState = mutedConversationStatus)
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

@Composable
fun rememberOtherUserProfileSheetState(
    conversationSheetContent: ConversationSheetContent?,
    groupState: OtherUserProfileGroupState?
): OtherUserBottomSheetState {
    // MutedConversationStatus may be changed and it will cause re-composing the whole BottomSheet, which we don't want.
    // So we use always the same MutedConversationStatus in remembering-key
    val ignoreMutedConversationStatusKey =
        conversationSheetContent?.copy(mutingConversationState = MutedConversationStatus.AllMuted)

    return remember(ignoreMutedConversationStatusKey, groupState) {
        OtherUserBottomSheetState(
            conversationSheetContent = conversationSheetContent,
            groupState = groupState
        )
    }
}
