package com.wire.android.ui.home.conversationslist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId


@Composable
fun SearchConversationScreen(
    conversationSearchResult: Map<ConversationFolder, List<ConversationItem>>,
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
            val context = LocalContext.current

            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize()
            ) {
                /*
             * When the list is scrolled to top and new items (e.g. new activity section) should appear on top of the list, it appears above
             * all current items, scroll is preserved so the list still shows the same item as the first one on list so it scrolls
             * automatically to that item and the newly added section on top is hidden above this previously top item, so for such situation
             * when the list is scrolled to the top and we want the new section to appear at the top we need a dummy top item which will make
             *  it so it wants to keep this dummy top item as the first one on list and show all other items below it.
             */
                item("empty-top-header") {
                    Divider(
                        thickness = Dp.Hairline,
                        color = Color.Transparent
                    )
                }
                conversationSearchResult.forEach { (conversationFolder, conversationList) ->
                    folderWithElements(
                        header = when (conversationFolder) {
                            is ConversationFolder.Predefined -> context.getString(conversationFolder.folderNameResId)
                            is ConversationFolder.Custom -> conversationFolder.folderName
                        },
                        items = conversationList.associateBy {
                            it.conversationId.toString()
                        }
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
                "No conversations could be found.",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                textAlign = TextAlign.Center
            )
            VerticalSpace.x16()
            Text(
                "Connect with new users or start a new conversation:",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                textAlign = TextAlign.Center
            )
        }
        VerticalSpace.x16()
        WirePrimaryButton(
            text = "New converastion",
            fillMaxWidth = false,
            minHeight = 32.dp,
            onClick = onNewConversationCLick
        )
    }
}
