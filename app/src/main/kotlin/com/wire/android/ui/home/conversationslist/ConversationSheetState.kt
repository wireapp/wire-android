package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationOptionNavigation
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

class ConversationSheetState(
    conversationSheetContent: ConversationSheetContent? = null,
    conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
) {

    var conversationSheetContent: ConversationSheetContent? by mutableStateOf(conversationSheetContent)

    var currentOptionNavigation: ConversationOptionNavigation by mutableStateOf(conversationOptionNavigation)

    val conversationId: ConversationId?
        get() = conversationSheetContent?.conversationId

    fun muteConversation(mutedConversationStatus: MutedConversationStatus) {
        conversationSheetContent = conversationSheetContent?.copy(mutingConversationState = mutedConversationStatus)
    }

    fun toMutingNotificationOption() {
        currentOptionNavigation = ConversationOptionNavigation.MutingNotificationOption
    }

    fun toHome() {
        currentOptionNavigation = ConversationOptionNavigation.Home
    }
}

@Composable
fun rememberConversationSheetState(
    conversationItem: ConversationItem,
    conversationOptionNavigation: ConversationOptionNavigation
): ConversationSheetState {
    val conversationSheetContent: ConversationSheetContent = when (conversationItem) {
        is ConversationItem.GroupConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = groupName.ifEmpty { stringResource(id = R.string.default_deleted_username) },
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Group(
                        conversationId = conversationId
                    )
                )
            }
        }
        is ConversationItem.PrivateConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = conversationInfo.name,
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Private(
                        userAvatarData.asset
                    )
                )
            }
        }
        is ConversationItem.ConnectionConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = conversationInfo.name,
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Private(
                        userAvatarData.asset
                    )
                )
            }
        }
    }

    return remember(conversationItem, conversationOptionNavigation) {
        ConversationSheetState(
            conversationSheetContent = conversationSheetContent,
            conversationOptionNavigation = conversationOptionNavigation
        )
    }
}
