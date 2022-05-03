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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.common.loading.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.newconversation.common.GroupButton
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireTypography

@Composable
fun ContactsScreen(
    allKnownContact: List<Contact>,
    contactsAddedToGroup: List<Contact>,
    onOpenUserProfile: (Contact) -> Unit,
    onAddToGroup: (Contact) -> Unit,
    onRemoveFromGroup: (Contact) -> Unit,
    onScrollPositionChanged: (Int) -> Unit,
    onNewGroupClicked: () -> Unit
) {
    val lazyListState = rememberLazyListState { itemIndex ->
        onScrollPositionChanged(itemIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
    ) {
        if (false) {
            CenteredCircularProgressBarIndicator()
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
            ) {
                folderWithElements(
                    header = { stringResource(R.string.label_contacts) },
                    items = allKnownContact
                ) { contact ->
                    ContactItem(
                        name = contact.name,
                        avatarAsset = contact.avatarAsset,
                        userStatus = contact.userStatus,
                        belongsToGroup = contactsAddedToGroup.contains(contact),
                        addToGroup = { onAddToGroup(contact) },
                        removeFromGroup = { onRemoveFromGroup(contact) },
                        openUserProfile = { onOpenUserProfile(contact) }
                    )
                }
            }
            Divider()
            GroupButton(groupSize = contactsAddedToGroup.size, onNewGroupClicked = onNewGroupClicked)
        }
    }
}

@Composable
private fun ContactItem(
    name: String,
    avatarAsset: UserAvatarAsset?,
    userStatus: UserStatus,
    belongsToGroup: Boolean,
    addToGroup: () -> Unit,
    removeFromGroup: () -> Unit,
    openUserProfile: () -> Unit,
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                WireCheckbox(checked = belongsToGroup, onCheckedChange = { if (it) addToGroup() else removeFromGroup() })
                UserProfileAvatar(
                    userAvatarAsset = avatarAsset,
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
        onRowItemClicked = openUserProfile,
        onRowItemLongClicked = {
            // TODO: implement later on
        }
    )
}
