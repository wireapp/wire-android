package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@ExperimentalMaterialApi
class ConversationState {
    var conversationSheetContent: ConversationSheetContent? by mutableStateOf(null)

    fun changeModalSheetContentState(conversationType: ConversationType) {
        when (conversationType) {
            is ConversationType.GroupConversation -> {
                with(conversationType) {
                    conversationSheetContent = ConversationSheetContent(
                        conversationId = conversationId,
                        title = groupName,
                        mutingConversationState = mutedStatus,
                        conversationTypeDetail = ConversationTypeDetail.Group(conversationId = conversationId)
                    )
                }
            }
            is ConversationType.PrivateConversation -> {
                with(conversationType) {
                    conversationSheetContent = ConversationSheetContent(
                        conversationId = conversationId,
                        title = conversationInfo.name,
                        mutingConversationState = mutedStatus,
                        conversationTypeDetail = ConversationTypeDetail.Private(userInfo.avatarAsset)
                    )
                }
            }
        }
    }

    fun muteConversation(mutedConversationStatus: MutedConversationStatus) {
        conversationSheetContent = conversationSheetContent?.copy(mutingConversationState = mutedConversationStatus)
    }
}
