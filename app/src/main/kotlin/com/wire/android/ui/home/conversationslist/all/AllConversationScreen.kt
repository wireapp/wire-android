package com.wire.android.ui.home.conversationslist.all

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun AllConversationScreen(
    conversations: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    hasNoConversations: Boolean,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onOpenConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val lazyListState = rememberLazyListState()

    if (hasNoConversations) {
        ConversationListEmptyStateScreen()
    } else {
        ConversationList(
            lazyListState = lazyListState,
            conversationListItems = conversations,
            searchQuery = "",
            onOpenConversation = onOpenConversation,
            onEditConversation = onEditConversation,
            onOpenUserProfile = onOpenUserProfile,
            onOpenConversationNotificationsSettings = onOpenConversationNotificationsSettings,
            onJoinCall = onJoinCall
        )
    }
}

@Composable
fun ConversationListEmptyStateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                dimensions().spacing40x
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(
                bottom = dimensions().spacing24x,
                top = dimensions().spacing100x
            ),
            text = stringResource(R.string.conversation_empty_list_title),
            style = MaterialTheme.wireTypography.title01,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Text(
            modifier = Modifier.padding(bottom = dimensions().spacing8x),
            text = stringResource(R.string.conversation_empty_list_description),
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Image(
            modifier = Modifier.padding(start = dimensions().spacing100x),
            painter = painterResource(
                id = R.drawable.ic_empty_conversation_arrow
            ),
            contentDescription = ""
        )
    }
}


@Preview
@Composable
fun ComposablePreview() {
    AllConversationScreen(
        conversations = persistentMapOf(),
        hasNoConversations = false,
        onOpenConversation = {},
        onEditConversation = {},
        onOpenUserProfile = {},
        onOpenConversationNotificationsSettings = {},
        onJoinCall = {}
    )
}

@Preview
@Composable
fun ConversationListEmptyStateScreenPreview() {
    AllConversationScreen(
        conversations = persistentMapOf(),
        hasNoConversations = true,
        onOpenConversation = {},
        onEditConversation = {},
        onOpenUserProfile = {},
        onOpenConversationNotificationsSettings = {},
        onJoinCall = {}
    )
}
