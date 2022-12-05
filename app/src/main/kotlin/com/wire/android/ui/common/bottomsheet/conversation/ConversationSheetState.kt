package com.wire.android.ui.common.bottomsheet.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

class ConversationSheetState(
    conversationSheetContent: ConversationSheetContent? = null,
    conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
) {
    val startOptionNavigation = conversationOptionNavigation

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
                    title = groupName.ifEmpty { stringResource(id = R.string.member_name_deleted_label) },
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Group(
                        conversationId = conversationId,
                        isCreator = isSelfUserCreator
                    ),
                    isSelfUserMember = isSelfUserMember,
                    isTeamConversation = teamId != null
                )
            }
        }
        is ConversationItem.PrivateConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = if (conversationInfo.unavailable) {
                        stringResource(id = R.string.username_unavailable_label)
                    } else conversationInfo.name,
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Private(
                        userAvatarData.asset,
                        userId,
                        blockingState
                    ),
                    isTeamConversation = isTeamConversation
                )
            }
        }
        is ConversationItem.ConnectionConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = conversationInfo.name,
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Connection(
                        userAvatarData.asset
                    ),
                    isTeamConversation = isTeamConversation
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

@Composable
fun rememberConversationSheetState(
    conversationSheetContent: ConversationSheetContent?,
    conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
): ConversationSheetState {
    return remember(conversationSheetContent) {
        ConversationSheetState(
            conversationSheetContent = conversationSheetContent,
            conversationOptionNavigation = conversationOptionNavigation
        )
    }
}
