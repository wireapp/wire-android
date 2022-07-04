package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.model.Clickable
import com.wire.android.ui.common.AddContactButton
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership

@Composable
fun InternalContactSearchResultItem(
    avatarData: UserAvatarData,
    name: String,
    label: String,
    membership: Membership,
    searchQuery: String,
    addToGroup: () -> Unit,
    removeFromGroup: () -> Unit,
    isAddedToGroup: Boolean,
    clickable: Clickable,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                WireCheckbox(
                    checked = isAddedToGroup,
                    onCheckedChange = { if (it) addToGroup() else removeFromGroup() }
                )
                UserProfileAvatar(avatarData)
            }
        },
        title = {
            Row {
                HighlightName(
                    name = name,
                    searchQuery = searchQuery
                )
                Spacer(Modifier.width(dimensions().spacing8x))
                MembershipQualifierLabel(membership)
            }
        },
        subtitle = {
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
        clickable = clickable,
        modifier = modifier
    )
}

@Composable
fun ExternalContactSearchResultItem(
    avatarData: UserAvatarData,
    name: String,
    label: String,
    membership: Membership,
    searchQuery: String,
    isConnectedOrPending: Boolean,
    clickable: Clickable,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                UserProfileAvatar(avatarData)
            }
        },
        title = {
            Row {
                HighlightName(
                    name = name,
                    searchQuery = searchQuery
                )
                Spacer(Modifier.width(dimensions().spacing8x))
                MembershipQualifierLabel(membership)
            }
        },
        subtitle = {
            HighlightSubtitle(
                subTitle = label,
                searchQuery = searchQuery
            )
        },
        actions = {
            if (!isConnectedOrPending) AddContactButton({ })
        },
        clickable = clickable,
        modifier = modifier
    )
}

