package com.wire.android.ui.home.conversationslist.bottomsheet

import MutingOptionsSheetContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.getMutedStatusTextResource
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun ConversationSheetContent(
    conversationSheetContent: ConversationSheetContent,
    mutedStatus: MutedConversationStatus,
    muteConversation: () -> Unit,
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
                mutedStatus = mutedStatus,
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
                onBackClick = { conversationOptionSheetState.toHome() },
            )
        }
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

sealed class ConversationSheetContent(val title: String, val conversationId: ConversationId?, var mutedStatus: MutedConversationStatus) {

    object Initial : ConversationSheetContent(
        title = "",
        conversationId = null,
        mutedStatus = MutedConversationStatus.AllAllowed
    )

    class PrivateConversation(
        title: String,
        val avatarAsset: UserAvatarAsset?,
        conversationId: ConversationId,
        mutedStatus: MutedConversationStatus
    ) : ConversationSheetContent(
        title = title,
        conversationId = conversationId,
        mutedStatus = mutedStatus
    )

    class GroupConversation(
        title: String,
        val groupColorValue: Long,
        conversationId: ConversationId,
        mutedStatus: MutedConversationStatus
    ) : ConversationSheetContent(
        title = title,
        conversationId = conversationId,
        mutedStatus = mutedStatus
    )
//
//    fun updateCurrentEditingMutedStatus(mutedStatus: MutedConversationStatus) {
//        this.mutedStatus = mutedStatus
//    }
}

internal sealed class ConversationOptionNavigation {
    object Home : ConversationOptionNavigation()
    object MutingNotificationOption : ConversationOptionNavigation()
}


//data class NotificationsOptionsItem(
//    val muteConversationAction: () -> Unit,
//    val mutedStatus: MutedConversationStatus
//)
