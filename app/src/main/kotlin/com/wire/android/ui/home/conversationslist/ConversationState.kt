package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@ExperimentalMaterialApi
class ConversationState {
    var conversationSheetContent: ConversationSheetContent? by mutableStateOf(null)

    fun changeModalSheetContentState(conversationType: ConversationItem) {
        when (conversationType) {
            is ConversationItem.GroupConversation -> {
                with(conversationType) {
                    conversationSheetContent = ConversationSheetContent(
                        conversationId = conversationId,
                        title = groupName,
                        mutingConversationState = mutedStatus,
                        conversationTypeDetail = ConversationTypeDetail.Group(conversationId = conversationId)
                    )
                }
            }
            is ConversationItem.PrivateConversation -> {
                with(conversationType) {
                    conversationSheetContent = ConversationSheetContent(
                        conversationId = conversationId,
                        title = conversationInfo.name,
                        mutingConversationState = mutedStatus,
                        conversationTypeDetail = ConversationTypeDetail.Private(userInfo.avatarAsset)
                    )
                }
            }
            is ConversationItem.ConnectionConversation -> {
                // TODO should we have some options for connection requests?
            }
        }
    }

    fun muteConversation(mutedConversationStatus: MutedConversationStatus) {
        conversationSheetContent = conversationSheetContent?.copy(mutingConversationState = mutedConversationStatus)
    }
}
