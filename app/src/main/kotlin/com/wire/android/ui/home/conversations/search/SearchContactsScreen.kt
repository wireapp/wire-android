/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversations.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SearchContactsScreen(
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
                                openUserProfile = { onOpenUserProfile(this) },
                                isMetadataNotAvailable = isMetadataEmpty()
                            )
                        }
                    }
                }
            }
        }

        is SearchResultState.Failure -> {
            SearchFailureBox(failureMessage = allKnownContactResult.failureString)
        }

        SearchResultState.EmptyResult -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensions().spacing40x),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_empty_contacts_arrow),
                    contentDescription = ""
                )
                Text(
                    modifier = Modifier.padding(top = dimensions().spacing16x),
                    text = stringResource(R.string.label_empty_contacts_list),
                    style = MaterialTheme.wireTypography.body01,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.wireColorScheme.onSurface,
                )
            }
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
    isMetadataNotAvailable: Boolean = false
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
                    text = if (isMetadataNotAvailable) stringResource(R.string.username_unavailable_label) else name,
                    style = MaterialTheme.wireTypography.title02.copy(
                        color = if (isMetadataNotAvailable) MaterialTheme.wireColorScheme.secondaryText
                        else MaterialTheme.wireTypography.title02.color
                    ),
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
                    .padding(end = dimensions().spacing8x)
            ) {
                ArrowRightIcon(Modifier.align(Alignment.TopEnd))
            }
        },
        clickable = clickable
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewContactItem() {
    WireTheme {
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
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchContactsScreen() {
    WireTheme {
        SearchContactsScreen(
            allKnownContactResult = SearchResultState.Success(
                persistentListOf(
                    Contact(
                        id = "1",
                        domain = "domain",
                        name = "Name",
                        avatarData = UserAvatarData(),
                        membership = Membership.Admin,
                        connectionState = ConnectionState.ACCEPTED
                    )
                )
            ),
            contactsAddedToGroup = listOf(),
            onOpenUserProfile = { },
            onAddToGroup = { },
            onRemoveFromGroup = { }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchContactsScreenEmpty() {
    WireTheme {
        SearchContactsScreen(
            allKnownContactResult = SearchResultState.EmptyResult,
            contactsAddedToGroup = listOf(),
            onOpenUserProfile = { },
            onAddToGroup = { },
            onRemoveFromGroup = { }
        )
    }
}
