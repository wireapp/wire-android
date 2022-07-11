package com.wire.android.ui.home.conversationslist.bottomsheet

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversationslist.ConversationState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConversationSheetContent(
    conversationState : ConversationState,
    onMutingConversationStatusChange: (MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: () -> Unit,
    leaveGroup: () -> Unit
) {
    when (conversationState.currentOptionNavigation) {
        ConversationOptionNavigation.Home -> {
            HomeSheetContent(
                conversationSheetContent = conversationState.conversationSheetContent!!,
                addConversationToFavourites = addConversationToFavourites,
                moveConversationToFolder = moveConversationToFolder,
                moveConversationToArchive = moveConversationToArchive,
                clearConversationContent = clearConversationContent,
                blockUser = blockUser,
                leaveGroup = leaveGroup,
                navigateToNotification = conversationState::toMutingNotificationOption
            )
        }
        ConversationOptionNavigation.MutingNotificationOption -> {
            MutingOptionsSheetContent(
                mutingConversationState = conversationState.conversationSheetContent!!.mutingConversationState,
                onMuteConversation = onMutingConversationStatusChange,
                onBackClick = conversationState::toHome
            )
        }
    }

    BackHandler(conversationState.currentOptionNavigation is ConversationOptionNavigation.MutingNotificationOption) {
        conversationState.toHome()
    }
}

sealed class ConversationOptionNavigation {
    object Home : ConversationOptionNavigation()
    object MutingNotificationOption : ConversationOptionNavigation()
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
