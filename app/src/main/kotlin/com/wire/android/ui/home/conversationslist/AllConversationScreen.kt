package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.NewActivity
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<ConversationItem>>,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onOpenConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val lazyListState = rememberLazyListState()

    AllConversationContent(
        lazyListState = lazyListState,
        newActivities = newActivities,
        conversations = conversations,
        onOpenConversation = onOpenConversation,
        onEditConversation = onEditConversation,
        onOpenUserProfile = onOpenUserProfile,
        onOpenConversationNotificationsSettings = onOpenConversationNotificationsSettings,
        onJoinCall = onJoinCall
    )
}

@Composable
private fun AllConversationContent(
    lazyListState: LazyListState,
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<ConversationItem>>,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onOpenConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = context.getString(R.string.conversation_label_new_activity),
            items = newActivities.associateBy { it.conversationItem.conversationId.toString() }
        ) { newActivity ->
            with(newActivity) {
                ConversationItemFactory(
                    conversation = conversationItem,
                    eventType = eventType,
                    openConversation = onOpenConversation,
                    openMenu = onEditConversation,
                    openUserProfile = onOpenUserProfile,
                    openNotificationsOptions = onOpenConversationNotificationsSettings,
                    joinCall = onJoinCall
                )
            }
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = when (conversationFolder) {
                    is ConversationFolder.Predefined -> context.getString(conversationFolder.folderNameResId)
                    is ConversationFolder.Custom -> conversationFolder.folderName
                },
                items = conversationList.associateBy { it.conversationId.toString() }
            ) { generalConversation ->
                ConversationItemFactory(
                    conversation = generalConversation,
                    openConversation = onOpenConversation,
                    openMenu = onEditConversation,
                    openUserProfile = onOpenUserProfile,
                    openNotificationsOptions = onOpenConversationNotificationsSettings,
                    joinCall = onJoinCall
                )
            }
        }
    }
}

@Preview
@Composable
fun ComposablePreview() {
    AllConversationScreen(listOf(), mapOf(), {}, {}, {}, {}, {})
}
