package com.wire.android.ui.home.newconversation.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WiredCheckbox
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.newconversation.common.GroupButton
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
    val lazyListState = rememberLazyListState {
        onScrollPositionChanged(it)
    }

    val contactsScreenState = rememberContactScreenState()

    with(contactsScreenState) {
        Column(
            Modifier
                .fillMaxSize()
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
                        name = contact.name,
                        userStatus = contact.userStatus,
                        belongsToGroup = newGroupContacts.contains(contact),
                        addToGroup = { addContactToGroup(contact) },
                        removeFromGroup = { removeContactFromGroup(contact) }
                    )
                }
            }
            Divider()
            GroupButton(groupSize = contactsScreenState.newGroupContacts.size)
        }
    }
}

@Composable
private fun ContactItem(
    name: String,
    userStatus: UserStatus,
    belongsToGroup: Boolean,
    addToGroup: () -> Unit,
    removeFromGroup: () -> Unit,
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                WiredCheckbox(checked = belongsToGroup, onCheckedChange = { if (it) addToGroup() else removeFromGroup() })
                UserProfileAvatar(
                    status = userStatus
                )
            }
        },
        title = {
            Text(
                text = name,
                style = MaterialTheme.wireTypography.title02,
            )
        },

        actions = {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(end = 8.dp)
            ) {
                ArrowRightIcon(Modifier.align(Alignment.TopEnd))
            }
        },
        onRowItemClicked = { },
        onRowItemLongClicked = { }
    )
}

class ContactsScreenState {

    val newGroupContacts = mutableStateListOf<Contact>()

    fun addContactToGroup(contact: Contact) {
        newGroupContacts.add(contact)
    }

    fun removeContactFromGroup(contact: Contact) {
        newGroupContacts.remove(contact)
    }

}

@Composable
fun rememberContactScreenState(): ContactsScreenState {
    return remember {
        ContactsScreenState()
    }
}
