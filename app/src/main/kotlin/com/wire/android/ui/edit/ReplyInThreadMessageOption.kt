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
package com.wire.android.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon

@Composable
fun ReplyInThreadMessageOption(onReplyInThreadClick: () -> Unit) {
    MenuBottomSheetItem(
        leading = {
            MenuItemIcon(
                id = R.drawable.ic_unread_reply,
                contentDescription = stringResource(R.string.content_description_reply_in_thread),
            )
        },
        title = stringResource(R.string.label_reply_in_thread),
        onItemClick = onReplyInThreadClick,
    )
}
