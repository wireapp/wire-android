/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.model.Clickable
import com.wire.android.model.ItemActionType
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.AddContactButton
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.ConnectPendingRequestBadge
import com.wire.android.ui.home.conversationslist.common.ConnectRequestBadge
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

@Composable
fun InternalContactSearchResultItem(
    avatarData: UserAvatarData,
    name: String,
    label: String,
    membership: Membership,
    searchQuery: String,
    connectionState: ConnectionState,
    onCheckClickable: Clickable,
    isSelected: Boolean,
    clickable: Clickable,
    actionType: ItemActionType,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = { UserProfileAvatar(avatarData) },
        titleStartPadding = dimensions().spacing0x,
        title = {
            Row(verticalAlignment = CenterVertically) {
                HighlightName(
                    name = name,
                    searchQuery = searchQuery,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                UserBadge(
                    membership = membership,
                    connectionState = connectionState,
                    startPadding = dimensions().spacing8x
                )
            }
        },
        subtitle = {
            HighlightSubtitle(
                subTitle = label,
                searchQuery = searchQuery
            )
        },
        actions = {
            if (actionType.clickable) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(end = dimensions().spacing4x)
                ) {
                    ArrowRightIcon(Modifier.align(Alignment.TopEnd), R.string.content_description_empty)
                }
            } else if (actionType.checkable) {
                WireCheckbox(
                    checked = isSelected,
                    onCheckedChange = null, // null since we are handling the click on parent,
                )
            }
        },
        clickable =
        if (actionType.clickable) {
            clickable
        } else {
            onCheckClickable
        },
        modifier = modifier
            .padding(start = dimensions().spacing8x)
            .semantics {
                if (actionType.checkable && isSelected) selected = true
            }
    )
}

@Composable
fun ExternalContactSearchResultItem(
    avatarData: UserAvatarData,
    userId: UserId,
    name: String,
    label: String,
    membership: Membership,
    searchQuery: String,
    connectionState: ConnectionState,
    clickable: Clickable,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row {
                UserProfileAvatar(avatarData)
            }
        },
        titleStartPadding = dimensions().spacing0x,
        title = {
            Row(verticalAlignment = CenterVertically) {
                HighlightName(
                    name = name,
                    searchQuery = searchQuery,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                UserBadge(
                    membership = membership,
                    connectionState = connectionState,
                    startPadding = dimensions().spacing8x
                )
            }
        },
        subtitle = {
            HighlightSubtitle(
                subTitle = label,
                searchQuery = searchQuery
            )
        },
        actions = {
            when (connectionState) {
                ConnectionState.NOT_CONNECTED, ConnectionState.CANCELLED ->
                    AddContactButton(userId, name)

                ConnectionState.PENDING, ConnectionState.IGNORED ->
                    Box(modifier = Modifier.padding(horizontal = dimensions().spacing12x)) { ConnectRequestBadge() }

                ConnectionState.SENT ->
                    Box(modifier = Modifier.padding(horizontal = dimensions().spacing12x)) { ConnectPendingRequestBadge() }

                ConnectionState.BLOCKED -> {
                }

                ConnectionState.MISSING_LEGALHOLD_CONSENT -> {
                    appLogger.e("Unhandled ConnectionState.MISSING_LEGALHOLD_CONSENT in ExternalContactSearchResultItem")
                }

                ConnectionState.ACCEPTED -> {
                    appLogger.e("ConnectionState.ACCEPTED should not appear in ExternalContactSearchResultItem")
                }
            }
        },
        clickable = clickable,
        modifier = modifier.padding(start = dimensions().spacing8x)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewInternalContactSearchResultItemCheckable() = WireTheme {
    InternalContactSearchResultItem(
        avatarData = UserAvatarData(),
        name = "John Doe",
        label = "label",
        membership = Membership.None,
        searchQuery = "",
        connectionState = ConnectionState.ACCEPTED,
        onCheckClickable = Clickable {},
        isSelected = false,
        clickable = Clickable {},
        actionType = ItemActionType.CHECK,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewInternalContactSearchResultItemClickable() = WireTheme {
    InternalContactSearchResultItem(
        avatarData = UserAvatarData(),
        name = "John Doe",
        label = "label",
        membership = Membership.None,
        searchQuery = "",
        connectionState = ConnectionState.ACCEPTED,
        onCheckClickable = Clickable {},
        isSelected = false,
        clickable = Clickable {},
        actionType = ItemActionType.CLICK,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewExternalContactSearchResultItem() = WireTheme {
    ExternalContactSearchResultItem(
        avatarData = UserAvatarData(),
        userId = UserId("id", "domain"),
        name = "John Doe",
        label = "label",
        membership = Membership.None,
        searchQuery = "",
        connectionState = ConnectionState.NOT_CONNECTED,
        clickable = Clickable {},
    )
}
