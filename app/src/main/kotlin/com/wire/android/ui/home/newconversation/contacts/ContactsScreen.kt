package com.wire.android.ui.home.newconversation.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.loading.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversations.search.SearchResultState
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun ContactsScreen(
    allKnownContactResult: SearchResultState,
    contactsAddedToGroup: List<Contact>,
    onOpenUserProfile: (Contact) -> Unit,
    onAddToGroup: (Contact) -> Unit,
    onRemoveFromGroup: (Contact) -> Unit
) {
    when (allKnownContactResult) {
        SearchResultState.Initial, SearchResultState.InProgress -> {
            CenteredCircularProgressBarIndicator()
        }
        is SearchResultState.Success -> {
            val lazyListState = rememberLazyListState()

            val context = LocalContext.current
            Column(
                Modifier
                    .fillMaxSize()
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                ) {
                    folderWithElements(
                        header = context.getString(R.string.label_contacts),
                        items = allKnownContactResult.result.associateBy { it.id }
                    ) { contact ->
                        with(contact) {
                            ContactItem(
                                name = name,
                                avatarData = avatarData,
                                membership = membership,
                                connectionState = connectionState,
                                belongsToGroup = contactsAddedToGroup.contains(this),
                                addToGroup = { onAddToGroup(this) },
                                removeFromGroup = { onRemoveFromGroup(this) },
                                openUserProfile = { onOpenUserProfile(this) }
                            )
                        }
                    }
                }
            }
        }
        is SearchResultState.Failure -> {
            SearchFailureBox(failureMessage = allKnownContactResult.failureString)
        }
    }
}

@Composable
private fun ContactItem(
    name: String,
    avatarData: UserAvatarData,
    membership: Membership,
    belongsToGroup: Boolean,
    connectionState: ConnectionState,
    addToGroup: () -> Unit,
    removeFromGroup: () -> Unit,
    openUserProfile: () -> Unit,
) {
    val clickable = remember {
        Clickable(
            enabled = true,
            onClick = openUserProfile,
            onLongClick = {
                // TODO: implement later on
            })
    }
    RowItemTemplate(
        leadingIcon = {
            Row {
                WireCheckbox(
                    checked = belongsToGroup,
                    onCheckedChange = { isChecked -> if (isChecked) addToGroup() else removeFromGroup() })
                UserProfileAvatar(avatarData)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.wireTypography.title02,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                UserBadge(
                    membership = membership,
                    connectionState = connectionState,
                    startPadding = dimensions().spacing8x
                )
            }
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
        clickable = clickable
    )
}

@Preview
@Composable
fun ContactItemPreview() {
    ContactItem(
        name = "Name",
        avatarData = UserAvatarData(),
        membership = Membership.Admin,
        belongsToGroup = true,
        connectionState = ConnectionState.ACCEPTED,
        addToGroup = { },
        removeFromGroup = { },
        openUserProfile = { }
    )
}
