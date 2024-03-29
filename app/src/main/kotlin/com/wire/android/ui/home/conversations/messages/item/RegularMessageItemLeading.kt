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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.model.MessageHeader

@Composable
fun RegularMessageItemLeading(
    header: MessageHeader, showAuthor: Boolean,
    userAvatarData: UserAvatarData,
    isContentClickable: Boolean,
    onOpenProfile: (String) -> Unit
) {
    val isProfileRedirectEnabled =
        header.userId != null && !(header.isSenderDeleted || header.isSenderUnavailable)
    if (showAuthor) {
        val avatarClickable = remember {
            Clickable(enabled = isProfileRedirectEnabled) {
                onOpenProfile(header.userId!!.toString())
            }
        }
        // because avatar takes start padding we don't need to add padding to message item
        UserProfileAvatar(
            avatarData = userAvatarData,
            clickable = if (isContentClickable) null else avatarClickable
        )
    }
}
