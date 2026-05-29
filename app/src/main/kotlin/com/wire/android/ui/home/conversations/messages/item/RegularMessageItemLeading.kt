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
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType.WithIndicators
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSenderId
import com.wire.android.ui.common.R as commonR

@Composable
fun RegularMessageItemLeading(
    header: MessageHeader,
    showAuthor: Boolean,
    userAvatarData: UserAvatarData,
    onOpenProfile: (MessageSenderId) -> Unit
) {
    val isProfileRedirectEnabled =
        header.senderId != null && !(header.isSenderDeleted || header.isSenderUnavailable)
    if (showAuthor) {
        val openProfileDescription = stringResource(id = R.string.content_description_open_user_profile_label)
        val avatarClickable = remember(isProfileRedirectEnabled, header.userId, openProfileDescription, onOpenProfile) {
            Clickable(
                enabled = isProfileRedirectEnabled,
                onClickDescription = openProfileDescription
            ) {
                header.senderId?.let {
                    onOpenProfile(it)
                }
            }
        }
        val avatarContentDescription = listOfNotNull(
            stringResource(id = commonR.string.content_description_user_avatar),
            header.username.asString(),
        ).joinToString(", ")
        // because avatar takes start padding we don't need to add padding to message item
        UserProfileAvatar(
            avatarData = userAvatarData,
            clickable = avatarClickable,
            contentDescription = avatarContentDescription,
            type = header.guestExpiresAt?.let { WithIndicators.TemporaryUser(it) } ?: WithIndicators.RegularUser(false)
        )
    }
}
