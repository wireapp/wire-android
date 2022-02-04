package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType

@Composable
fun MentionScreen(
    unreadMentions: List<ConversationUnreadMention> = emptyList(),
    allMentions: List<ConversationUnreadMention> = emptyList(),
    onMentionItemClick: (String) -> Unit
) {
    MentionContent(
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MentionContent(
    unreadMentions: List<ConversationUnreadMention>,
    allMentions: List<ConversationUnreadMention>,
    onMentionItemClick: (String) -> Unit
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.mention_label_unread_mentions) },
            items = unreadMentions
        ) { unreadMention ->
            MentionConversationItem(
                mention = unreadMention,
                eventType = EventType.UnreadMention,
                onMentionItemClick = { onMentionItemClick("someId") }
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            MentionConversationItem(
                mention = mention,
                onMentionItemClick = { onMentionItemClick("someId") }
            )
        }
    }
}


