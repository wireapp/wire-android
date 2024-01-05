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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId

data class UiMention(
    val start: Int,
    val length: Int,
    val userId: UserId,
    val handler: String // name that should be displayed in a message
) {
    fun intoMessageMention() = MessageMention(start, length, userId, false) // We can never send a self mention message
}

@Composable
fun MembersMentionList(
    membersToMention: List<Contact>,
    searchQuery: String,
    onMentionPicked: (Contact) -> Unit,
    modifier: Modifier
) {
    if (membersToMention.isNotEmpty()) Divider()
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
                    Divider(
                        color = MaterialTheme.wireColorScheme.divider,
                        thickness = Dp.Hairline
                    )
                }
            }
        }
    }
}
