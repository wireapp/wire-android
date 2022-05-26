package com.wire.android.ui.home.conversationslist.bottomsheet

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun ConversationSheetContent(
    conversationSheetContent: ConversationSheetContent,
    onMutingConversationStatusChange: (MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: () -> Unit,
    leaveGroup: () -> Unit
) {
    val conversationOptionSheetState = remember(conversationSheetContent) {
        ConversationOptionSheetState()
    }

    when (conversationOptionSheetState.currentNavigation) {
        ConversationOptionNavigation.Home -> {
            HomeSheetContent(
                conversationSheetContent = conversationSheetContent,
                mutedStatus = conversationSheetContent.mutedStatus,
                addConversationToFavourites = addConversationToFavourites,
                moveConversationToFolder = moveConversationToFolder,
                moveConversationToArchive = moveConversationToArchive,
                clearConversationContent = clearConversationContent,
                blockUser = blockUser,
                leaveGroup = leaveGroup,
                navigateToNotification = conversationOptionSheetState::toMutingNotificationOption
            )
        }
        ConversationOptionNavigation.MutingNotificationOption -> {
            MutingOptionsSheetContent(
                mutingConversationState = conversationSheetContent.mutedStatus,
                onMuteConversation = onMutingConversationStatusChange,
                onBackClick = conversationOptionSheetState::toHome
            )
        }
    }

    BackHandler(conversationOptionSheetState.currentNavigation is ConversationOptionNavigation.MutingNotificationOption) {
        conversationOptionSheetState.toHome()
    }
}

internal class ConversationOptionSheetState {

    var currentNavigation: ConversationOptionNavigation by mutableStateOf(ConversationOptionNavigation.Home)
        private set

    fun toMutingNotificationOption() {
        currentNavigation = ConversationOptionNavigation.MutingNotificationOption
    }

    fun toHome() {
        currentNavigation = ConversationOptionNavigation.Home
    }

}

sealed class HeaderAvatar {
    data class Group(val groupColorValue: Long) : HeaderAvatar()
    data class Private(val avatarAsset: UserAvatarAsset?) : HeaderAvatar()
}

data class ConversationSheetContent(
    val title: String,
    val headerAvatar: HeaderAvatar,
    val conversationId: ConversationId?,
    val mutedStatus: MutedConversationStatus,
)

internal sealed class ConversationOptionNavigation {
    object Home : ConversationOptionNavigation()
    object MutingNotificationOption : ConversationOptionNavigation()
}
