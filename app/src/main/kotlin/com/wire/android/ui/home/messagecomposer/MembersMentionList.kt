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
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun MembersMentionList(
    membersToMention: List<Contact>,
    searchQuery: String,
    onMentionPicked: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    if (membersToMention.isNotEmpty()) {
        HorizontalDivider()
    }
    LazyColumn(
        modifier = modifier.background(colorsScheme().background),
        reverseLayout = true
    ) {
        membersToMention.forEach {
            if (it.membership != Membership.Service) {
                item {
                    MemberItemToMention(
                        avatarData = it.avatarData,
                        name = it.name,
                        label = it.label,
                        membership = it.membership,
                        clickable = Clickable { onMentionPicked(it) },
                        searchQuery = searchQuery,
                        modifier = Modifier
                    )
                    HorizontalDivider(
                        thickness = Dp.Hairline,
                        color = MaterialTheme.wireColorScheme.divider
                    )
                }
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun MembersMentionListPreview() {
    WireTheme {
        MembersMentionList(
            membersToMention = listOf(
                Contact(
                    id = "1",
                    domain = "domain",
                    name = "Marko Alonso",
                    handle = "john.doe",
                    label = "label",
                    membership = Membership.Admin,
                    connectionState = ConnectionState.ACCEPTED
                ),
                Contact(
                    id = "2",
                    domain = "domain",
                    name = "John Doe",
                    handle = "john.doe",
                    label = "label",
                    membership = Membership.Admin,
                    connectionState = ConnectionState.ACCEPTED
                )
            ),
            searchQuery = "John",
            onMentionPicked = {},
            modifier = Modifier
        )
    }
}
