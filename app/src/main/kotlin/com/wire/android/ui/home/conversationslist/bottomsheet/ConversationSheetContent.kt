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
                navigateToNotification = { conversationOptionSheetState.toMutingNotificationOption() }
            )
        }
        ConversationOptionNavigation.MutingNotificationOption -> {
            MutingOptionsSheetContent(
                mutingConversationState = conversationSheetContent.mutedStatus,
                onMuteConversation = onMutingConversationStatusChange,
                onBackClick = { conversationOptionSheetState.toHome() }
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

sealed class ConversationSheetContent(
    open val title: String,
    open val conversationId: ConversationId?,
    open val mutedStatus: MutedConversationStatus
) {
    abstract fun copy(
        title: String? = null,
        conversationId: ConversationId? = null,
        mutedStatus: MutedConversationStatus? = MutedConversationStatus.AllMuted
    ): ConversationSheetContent

    data class PrivateConversation(
        override var title: String,
        val avatarAsset: UserAvatarAsset?,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus
    ) : ConversationSheetContent(
        title = title,
        conversationId = conversationId,
        mutedStatus = mutedStatus
    ) {
        override fun copy(
            title: String?,
            conversationId: ConversationId?,
            mutedStatus: MutedConversationStatus?
        ): ConversationSheetContent {
            return copy(
                title = title ?: this.title,
                conversationId = conversationId ?: this.conversationId,
                mutedStatus = mutedStatus ?: this.mutedStatus
            )
        }
    }

    data class GroupConversation(
        override val title: String,
        val groupColorValue: Long,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus
    ) : ConversationSheetContent(
        title = title,
        conversationId = conversationId,
        mutedStatus = mutedStatus
    ) {
        override fun copy(
            title: String?,
            conversationId: ConversationId?,
            mutedStatus: MutedConversationStatus?
        ): ConversationSheetContent {
            return copy(
                title = title ?: this.title,
                conversationId = conversationId ?: this.conversationId,
                mutedStatus = mutedStatus ?: this.mutedStatus
            )
        }
    }

}

internal sealed class ConversationOptionNavigation {
    object Home : ConversationOptionNavigation()
    object MutingNotificationOption : ConversationOptionNavigation()
}
