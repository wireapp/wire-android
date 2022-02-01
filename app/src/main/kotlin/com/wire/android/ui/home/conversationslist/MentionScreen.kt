package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversations.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.common.folderWithElements
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.Mention


@Composable
fun MentionScreen(
    unreadMentions: List<Mention> = emptyList(),
    allMentions: List<Mention> = emptyList(),
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
    unreadMentions: List<Mention>,
    allMentions: List<Mention>,
    onMentionItemClick: (String) -> Unit
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.mention_label_unread_mentions) },
            items = unreadMentions
        ) { unreadMention ->
            ConversationItemFactory(
                item = unreadMention,
                eventType = EventType.MissedCall,
                onConversationItemClick = { onMentionItemClick("someId") }
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            ConversationItemFactory(
                item = mention,
                eventType = EventType.MissedCall,
                onConversationItemClick = { onMentionItemClick("someId") }
            )
        }
    }
}


