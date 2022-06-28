package com.wire.android.ui.home.newconversation.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.loading.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.common.GroupButton
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireTypography

@Composable
fun ContactsScreen(
    scrollPositionProvider: (() -> Int) -> Unit,
    allKnownContact: List<Contact>,
    contactsAddedToGroup: List<Contact>,
    onOpenUserProfile: (Contact) -> Unit,
    onAddToGroup: (Contact) -> Unit,
    onRemoveFromGroup: (Contact) -> Unit,
    onNewGroupClicked: () -> Unit
) {
    val lazyListState = rememberLazyListState()

    scrollPositionProvider { lazyListState.firstVisibleItemIndex }

    Column(
        Modifier
            .fillMaxSize()
    ) {
        if (false) {
            CenteredCircularProgressBarIndicator()
        } else {
            val context = LocalContext.current
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
            ) {
                folderWithElements(
                    header = context.getString(R.string.label_contacts),
                    items = allKnownContact.associateBy { it.id.toString() }
                ) { contact ->
                    with(contact) {
                        ContactItem(
                            name = name,
                            avatarData = avatarData,
                            membership = membership,
                            belongsToGroup = contactsAddedToGroup.contains(this),
                            addToGroup = { onAddToGroup(this) },
                            removeFromGroup = { onRemoveFromGroup(this) },
                            openUserProfile = { onOpenUserProfile(this) }
                        )
                    }
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
    avatarData: UserAvatarData,
    membership: Membership,
    belongsToGroup: Boolean,
    addToGroup: () -> Unit,
    removeFromGroup: () -> Unit,
    openUserProfile: () -> Unit,
) {
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
            Row {
                Text(
                    text = name,
                    style = MaterialTheme.wireTypography.title02,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(dimensions().spacing8x))
                MembershipQualifierLabel(membership = membership)
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
        onRowItemClicked = openUserProfile,
        onRowItemLongClicked = {
            // TODO: implement later on
        }
    )
}
