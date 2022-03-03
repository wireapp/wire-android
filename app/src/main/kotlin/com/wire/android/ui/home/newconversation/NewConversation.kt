package com.wire.android.ui.home.newconversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.theme.wireTypography


@Composable
fun NewConversationScreen(newConversationViewModel: NewConversationViewModel = hiltViewModel()) {
    val state by newConversationViewModel.newConversationState

    NewConversationContent(
        state = state,
        onCloseClick = { newConversationViewModel.close() }
    )
}

@Composable
fun NewConversationContent(state: NewConversationState, onCloseClick: () -> Unit) {
    val lazyListState = rememberLazyListState()

    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = dimensions().topBarSearchFieldHeight)
                .wrapContentSize()
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = dimensions().topBarSearchFieldHeight)
            ) {
                folderWithElements(
                    header = { "CONTACTS" },
                    items = state.contacts
                ) { contact ->
                    ContactItem(
                        contact.name,
                        contact.userStatus,
                        contact.avatarUrl
                    )
                }
            }
            Divider()
            Column(modifier = Modifier.padding(all = 16.dp)) {
                WirePrimaryButton(
                    text = "New Group",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                WirePrimaryButton(
                    text = "New Guestroom",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SearchableWireCenterAlignedTopAppBar(
            topBarTitle = "New Conversation",
            searchHint = "Search people",
            scrollPosition = lazyListState.firstVisibleItemIndex,
            onNavigationPressed = onCloseClick
        )
    }
}

@Composable
private fun ContactItem(
    name: String,
    status: UserStatus,
    avatarUrl: String
) {
    RowItem({
        //TODO: Open Contact Screen
    }, {
        //TODO: Show Context Menu ?
    }, {
        UserProfileAvatar(
            avatarUrl = avatarUrl,
            status = status
        )
        Text(
            text = name,
            style = MaterialTheme.wireTypography.title02,
        )
    })
}
