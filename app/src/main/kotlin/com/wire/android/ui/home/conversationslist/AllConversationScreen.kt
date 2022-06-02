package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.extension.rememberLazyListState
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
    onScrollPositionChanged: (Int) -> Unit = {},
) {
    val lazyListState = rememberLazyListState { firstVisibleItemIndex ->
        onScrollPositionChanged(firstVisibleItemIndex)
    }

    AllConversationContent(
        lazyListState = lazyListState,
        newActivities = newActivities,
        conversations = conversations,
        onOpenConversation = onOpenConversation,
        onEditConversation = onEditConversation,
        onOpenUserProfile = onOpenUserProfile,
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
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            with(newActivity) {
                ConversationItemFactory(
                    conversation = conversationItem,
                    eventType = eventType,
                    openConversation = onOpenConversation,
                    openMenu = onEditConversation,
                    openUserProfile = onOpenUserProfile,
                )
            }
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = {
                    when (conversationFolder) {
                        is ConversationFolder.Predefined -> stringResource(id = conversationFolder.folderNameResId)
                        is ConversationFolder.Custom -> conversationFolder.folderName
                    }
                },
                items = conversationList
            ) { generalConversation ->
                ConversationItemFactory(
                    conversation = generalConversation,
                    openConversation = onOpenConversation,
                    openMenu = onEditConversation,
                    openUserProfile = onOpenUserProfile,
                )
            }
        }
    }
}

@Preview
@Composable
fun ComposablePreview() {
    AllConversationScreen(listOf(), mapOf(), {}, {}, {}, {})
}
