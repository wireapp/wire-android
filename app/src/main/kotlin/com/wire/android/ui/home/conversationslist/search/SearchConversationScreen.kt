package com.wire.android.ui.home.conversationslist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap


@Composable
fun SearchConversationScreen(
    conversationSearchResult: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    onOpenNewConversation: () -> Unit,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onOpenConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (conversationSearchResult.values.isEmpty()) {
            EmptySearchResult(onOpenNewConversation)
        } else {
            ConversationList(
                conversationListItems = conversationSearchResult,
                onOpenConversation = onOpenConversation,
                onEditConversation = onEditConversation,
                onOpenUserProfile = onOpenUserProfile,
                onOpenConversationNotificationsSettings = onOpenConversationNotificationsSettings,
                onJoinCall = onJoinCall
            )
        }
    }
}

@Composable
private fun EmptySearchResult(onNewConversationCLick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        VerticalSpace.x8()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                .padding(horizontal = dimensions().spacing48x)
                .wrapContentHeight()
        ) {
            Text(
                text = stringResource(R.string.label_no_conversation_found),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                textAlign = TextAlign.Center
            )
            VerticalSpace.x16()
            Text(
                text = stringResource(R.string.label_connect_with_new_users),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                textAlign = TextAlign.Center
            )
        }
        VerticalSpace.x16()
        WirePrimaryButton(
            text = stringResource(R.string.label_new_conversation),
            fillMaxWidth = false,
            minHeight = dimensions().spacing32x,
            onClick = onNewConversationCLick
        )
    }
}
