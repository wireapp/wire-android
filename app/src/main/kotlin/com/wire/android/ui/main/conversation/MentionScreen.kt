package com.wire.android.ui.main.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversation.all.model.toUserInfoLabel
import com.wire.android.ui.main.conversation.common.FolderHeader
import com.wire.android.ui.main.conversation.common.RowItem
import com.wire.android.ui.main.conversation.common.UnreadMentionBadge
import com.wire.android.ui.main.conversation.common.UserLabel
import com.wire.android.ui.main.conversation.mention.model.Mention
import com.wire.android.ui.theme.subLine1


@Composable
fun MentionScreen(
    unreadMentions: List<Mention> = emptyList(),
    allMentions: List<Mention> = emptyList()
) {
    MentionContent(unreadMentions, allMentions)
}

@Composable
private fun MentionContent(unreadMentions: List<Mention>, allMentions: List<Mention>) {
    LazyColumn {
        if (unreadMentions.isNotEmpty()) {
            item { FolderHeader(name = stringResource(R.string.mention_label_unread_mentions)) }
            items(unreadMentions) { unreadMention ->
                UnreadMentionRowItem(unreadMention)
            }
        }

        if (allMentions.isNotEmpty()) {
            item { FolderHeader(name = stringResource(R.string.mention_label_all_mentions)) }
            items(allMentions) { mention ->
                AllMentionRowItem(mention)
            }
        }
    }
}

@Composable
fun UnreadMentionRowItem(unreadMention: Mention) {
    RowItem {
        MentionLabel(
            mention = unreadMention,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        )
        UnreadMentionBadge(
            modifier = Modifier
                .wrapContentWidth()
                .padding(start = 4.dp)
        )
    }
}

@Composable
fun AllMentionRowItem(mention: Mention) {
    RowItem {
        MentionLabel(
            mention = mention,
            modifier = Modifier.padding(end = 42.dp)
        )
    }
}

@Composable
fun MentionLabel(mention: Mention, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        with(mention) {
            UserProfileAvatar(avatarUrl = conversation.userInfo.avatarUrl, onClick = {})
            Column {
                UserLabel(conversation.toUserInfoLabel())
                Text(
                    text = mentionInfo.mentionMessage.toQuote(),
                    style = MaterialTheme.typography.subLine1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


