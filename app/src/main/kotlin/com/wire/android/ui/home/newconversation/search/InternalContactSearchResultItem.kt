package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.AddContactButton
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireCheckbox


@Composable
fun InternalContactSearchResultItem(
    //TODO : this will need refactor we are not using avatarUrl
    avatarUrl: String = "",
    userStatus: UserStatus,
    name: String,
    label: String,
    searchQuery: String,
    addToGroup: () -> Unit,
    removeFromGroup: () -> Unit,
    isAddedToGroup: Boolean,
    onRowItemClicked: () -> Unit,
    onRowItemLongClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                WireCheckbox(
                    checked = isAddedToGroup,
                    onCheckedChange = { if (it) addToGroup() else removeFromGroup() }
                )

                UserProfileAvatar(
                    status = userStatus
                )
            }
        },
        title = {
            HighlightName(
                name = name,
                searchQuery = searchQuery
            )
        },
        subTitle = {
            HighlightSubtitle(
                subTitle = label,
                searchQuery = searchQuery
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
        onRowItemClicked = onRowItemClicked,
        onRowItemLongClicked = onRowItemLongClicked,
        modifier = modifier
    )
}

@Composable
fun ExternalContactSearchResultItem(
    //TODO : this will need refactor we are not using avatarUrl
    avatarUrl: String = "",
    userStatus: UserStatus,
    name: String,
    label: String,
    searchQuery: String,
    onRowItemClicked: () -> Unit,
    onRowItemLongClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                UserProfileAvatar(
                    status = userStatus
                )
            }
        },
        title = {
            HighlightName(
                name = name,
                searchQuery = searchQuery
            )
        },
        subTitle = {
            HighlightSubtitle(
                subTitle = label,
                searchQuery = searchQuery
            )
        },
        actions = {
            AddContactButton({ })
        },
        onRowItemClicked = onRowItemClicked,
        onRowItemLongClicked = onRowItemLongClicked,
        modifier = modifier
    )
}

