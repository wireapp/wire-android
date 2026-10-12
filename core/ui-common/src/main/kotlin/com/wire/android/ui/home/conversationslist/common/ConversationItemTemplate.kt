/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.wire.android.model.Clickable
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.R
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.WireRadioButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.ConversationId

/** Shared conversation row used by the conversation list and conversation participants import. */
@Composable
fun ConversationItemTemplate(
    conversationId: ConversationId,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isChannel: Boolean = false,
    isPrivateChannel: Boolean = false,
    selectionMode: ConversationItemSelectionMode = ConversationItemSelectionMode.NONE,
    isChecked: Boolean = false,
    isEnabled: Boolean = true,
    onSelectionChanged: (Boolean) -> Unit = {},
    subtitle: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    clickable: Clickable = Clickable(enabled = false),
    avatar: @Composable () -> Unit = {
        if (isChannel) {
            ChannelConversationAvatar(conversationId = conversationId, isPrivateChannel = isPrivateChannel)
        } else {
            RegularGroupConversationAvatar(conversationId = conversationId)
        }
    },
) {
    val isSelectable = selectionMode != ConversationItemSelectionMode.NONE
    val leadingSize = MaterialTheme.wireDimensions.avatarDefaultSize +
            MaterialTheme.wireDimensions.avatarClickablePadding * 2
    RowItemTemplate(
        modifier = modifier
            .padding(start = dimensions().spacing8x)
            .semantics(mergeDescendants = true) {
                if (isSelectable) {
                    selected = isChecked
                    role = if (selectionMode == ConversationItemSelectionMode.MULTIPLE) Role.Checkbox else Role.RadioButton
                }
            },
        titleStartPadding = dimensions().spacing0x,
        leadingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConversationSelectionControl(selectionMode, isChecked, isEnabled, onSelectionChanged)
                Box(modifier = Modifier.size(leadingSize), contentAlignment = Alignment.Center) {
                    avatar()
                }
            }
        },
        title = title,
        subtitle = subtitle,
        actions = if (isSelectable) null else actions,
        clickable = conversationItemClickable(selectionMode, isChecked, isEnabled, onSelectionChanged, clickable),
    )
}

/** Uses selection clicks for selectable rows and the supplied action for regular rows. */
@Composable
private fun conversationItemClickable(
    selectionMode: ConversationItemSelectionMode,
    isChecked: Boolean,
    isEnabled: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    clickable: Clickable,
): Clickable = if (selectionMode != ConversationItemSelectionMode.NONE) {
    Clickable(
        enabled = isEnabled,
        clickBlockParams = ClickBlockParams(debounceClicks = false),
        onClickDescription = stringResource(
            if (isChecked) R.string.content_description_unselect_label else R.string.content_description_select_label
        ),
        onClick = { onSelectionChanged(selectionMode == ConversationItemSelectionMode.SINGLE || !isChecked) },
    )
} else {
    clickable
}

@Composable
private fun ConversationSelectionControl(
    selectionMode: ConversationItemSelectionMode,
    isChecked: Boolean,
    isEnabled: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
) {
    when (selectionMode) {
        ConversationItemSelectionMode.NONE -> Unit
        ConversationItemSelectionMode.SINGLE -> WireRadioButton(
            checked = isChecked,
            enabled = isEnabled,
            onButtonChecked = { onSelectionChanged(true) }
        )

        ConversationItemSelectionMode.MULTIPLE -> WireCheckbox(
            checked = isChecked,
            enabled = isEnabled,
            onCheckedChange = onSelectionChanged,
        )
    }
}

enum class ConversationItemSelectionMode { NONE, SINGLE, MULTIPLE }
