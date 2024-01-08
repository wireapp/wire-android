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

package com.wire.android.ui.home.conversations.details.participants

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.folderWithElements

fun LazyListScope.participantsFoldersWithElements(
    context: Context,
    state: GroupConversationParticipantsState,
    onRowItemClicked: (UIParticipant) -> Unit
) {
    folderWithElements(
        header = context.getString(R.string.conversation_details_group_admins, state.data.allAdminsCount),
        items = state.data.admins,
        onRowItemClicked = onRowItemClicked
    )
    folderWithElements(
        header = context.getString(R.string.conversation_details_group_members, state.data.allParticipantsCount),
        items = state.data.participants,
        onRowItemClicked = onRowItemClicked
    )
}

fun LazyListScope.folderWithElements(
    header: String,
    items: List<UIParticipant>,
    onRowItemClicked: (UIParticipant) -> Unit,
    showRightArrow: Boolean = true
) = folderWithElements(
    header = header,
    items = items.associateBy { it.id.toString() },
    factory = {
        ConversationParticipantItem(
            uiParticipant = it,
            clickable = remember { Clickable(enabled = true) { onRowItemClicked(it) } },
            showRightArrow = showRightArrow
        )
    },
    divider = {
        Divider(
            color = MaterialTheme.wireColorScheme.background,
            thickness = Dp.Hairline
        )
    }
)
