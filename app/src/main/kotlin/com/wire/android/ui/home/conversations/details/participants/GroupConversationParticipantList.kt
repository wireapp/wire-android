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
import androidx.compose.runtime.remember
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.util.extension.folderWithElements

fun LazyListScope.participantsFoldersWithElements(
    context: Context,
    state: GroupConversationParticipantsState,
    onRowItemClicked: (UIParticipant) -> Unit
) {
    folderWithElements(
        header = context.getString(R.string.conversation_details_group_admins, state.data.allAdminsCount),
        items = state.data.admins,
        onRowItemClicked = onRowItemClicked,
        folderContentDescription = context.getString(
            R.string.content_description_conversation_details_admins_header,
            state.data.allAdminsCount
        )
    )
    folderWithElements(
        header = context.getString(R.string.conversation_details_group_members, state.data.allParticipantsCount),
        items = state.data.participants,
        onRowItemClicked = onRowItemClicked,
        folderContentDescription = context.getString(
            R.string.content_description_conversation_details_members_header,
            state.data.allAdminsCount
        )
    )
}

fun LazyListScope.folderWithElements(
    header: String,
    items: List<UIParticipant>,
    onRowItemClicked: (UIParticipant) -> Unit,
    showRightArrow: Boolean = true,
    folderContentDescription: String? = null
) = folderWithElements(
    header = header,
    items = items.associateBy { it.id.toString() },
    animateItemPlacement = false,
    factory = {
        ConversationParticipantItem(
            uiParticipant = it,
            clickable = remember { Clickable(enabled = true) { onRowItemClicked(it) } },
            showRightArrow = showRightArrow
        )
    },
    divider = { WireDivider() },
    folderContentDescription = folderContentDescription
)

fun LazyListScope.folderWithElements(
    header: String,
    items: Map<String, UIParticipant>,
    onRowItemClicked: (UIParticipant) -> Unit,
    showRightArrow: Boolean = true,
    folderContentDescription: String? = null
) = folderWithElements(
    header = header,
    items = items,
    animateItemPlacement = false,
    factory = {
        ConversationParticipantItem(
            uiParticipant = it,
            clickable = remember { Clickable(enabled = true) { onRowItemClicked(it) } },
            showRightArrow = showRightArrow
        )
    },
    divider = { WireDivider() },
    folderContentDescription = folderContentDescription
)
