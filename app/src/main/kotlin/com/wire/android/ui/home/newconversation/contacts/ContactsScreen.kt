package com.wire.android.ui.home.newconversation.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.newconversation.ContactsState
import com.wire.android.ui.theme.wireTypography


@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onScrollPositionChanged: (Int) -> Unit
) {
    ContactsScreenContent(
        state = viewModel.contactsState,
        onScrollPositionChanged = onScrollPositionChanged
    )
}

@Composable
fun ContactsScreenContent(
    state: ContactsState,
    onScrollPositionChanged: (Int) -> Unit
) {
    val lazyListState = rememberLazyListState { it ->
        onScrollPositionChanged(it)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentSize()
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
        ) {
            folderWithElements(
                header = { stringResource(R.string.label_contacts) },
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
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(all = 16.dp)
        ) {
            WirePrimaryButton(
                text = stringResource(R.string.label_new_group),
                onClick = {
                    //TODO:open new group screen
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            WirePrimaryButton(
                text = stringResource(R.string.label_new_guestroom),
                onClick = {
                    //TODO:open new guestroom
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
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

