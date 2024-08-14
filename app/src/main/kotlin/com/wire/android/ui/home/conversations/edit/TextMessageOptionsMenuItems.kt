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
package com.wire.android.ui.home.conversations.edit

import androidx.compose.runtime.Composable
import com.wire.android.ui.edit.CopyItemMenuOption
import com.wire.android.ui.edit.DeleteItemMenuOption
import com.wire.android.ui.edit.EditMessageMenuOption
import com.wire.android.ui.edit.MessageDetailsMenuOption
import com.wire.android.ui.edit.ReactionOption
import com.wire.android.ui.edit.ReplyMessageOption

@Composable
fun textMessageEditMenuItems(
    isEphemeral: Boolean,
    isUploading: Boolean,
    isComposite: Boolean,
    isEditable: Boolean,
    isCopyable: Boolean,
    onDeleteClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onReplyClick: () -> Unit,
    onCopyClick: () -> Unit,
    onReactionClick: (emoji: String) -> Unit,
    onEditClick: (() -> Unit),
): List<@Composable () -> Unit> {
    return buildList {
        if (!isUploading) {
            if (!isEphemeral && !isComposite) add { ReactionOption(onReactionClick) }
            add { MessageDetailsMenuOption(onDetailsClick) }
            if (isCopyable) { add { CopyItemMenuOption(onCopyClick) } }
            if (!isEphemeral && !isComposite) add { ReplyMessageOption(onReplyClick) }
            if (isEditable) add { EditMessageMenuOption(onEditClick) }
        }
        add { DeleteItemMenuOption(onDeleteClick) }
    }
}
