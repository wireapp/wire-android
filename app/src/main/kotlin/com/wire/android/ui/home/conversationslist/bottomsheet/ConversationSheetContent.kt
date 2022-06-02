package com.wire.android.ui.home.conversationslist.bottomsheet

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun ConversationSheetContent(
    conversationSheetContent: ConversationSheetContent,
    conversationOptionSheetState: ConversationOptionSheetState,
    onMutingConversationStatusChange: (MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: () -> Unit,
    leaveGroup: () -> Unit
) {

    when (conversationOptionSheetState.currentNavigation) {
        ConversationOptionNavigation.Home -> {
            HomeSheetContent(
                conversationSheetContent = conversationSheetContent,
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
                mutingConversationState = conversationSheetContent.mutingConversationState,
                onMuteConversation = onMutingConversationStatusChange,
                onBackClick = conversationOptionSheetState::toHome
            )
        }
    }

    BackHandler(conversationOptionSheetState.currentNavigation is ConversationOptionNavigation.MutingNotificationOption) {
        conversationOptionSheetState.toHome()
    }
}

sealed class ConversationOptionNavigation {
    object Home : ConversationOptionNavigation()
    object MutingNotificationOption : ConversationOptionNavigation()
}

class ConversationOptionSheetState(initialNavigation: ConversationOptionNavigation) {

    var currentNavigation: ConversationOptionNavigation by mutableStateOf(initialNavigation)
        private set

    fun toMutingNotificationOption() {
        currentNavigation = ConversationOptionNavigation.MutingNotificationOption
    }

    fun toHome() {
        currentNavigation = ConversationOptionNavigation.Home
    }
}

sealed class ConversationTypeDetail {
    data class Group(val conversationId: ConversationId) : ConversationTypeDetail()
    data class Private(val avatarAsset: UserAvatarAsset?) : ConversationTypeDetail()
}

data class ConversationSheetContent(
    val title: String,
    val conversationId: ConversationId,
    val mutingConversationState: MutedConversationStatus,
    val conversationTypeDetail: ConversationTypeDetail,
)
