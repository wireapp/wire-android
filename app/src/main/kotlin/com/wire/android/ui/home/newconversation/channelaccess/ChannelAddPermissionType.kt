/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.newconversation.channelaccess

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group.Channel.ChannelAddPermission

enum class ChannelAddPermissionType(@StringRes val labelResId: Int) {
    ADMINS(R.string.channel_add_permission_admin_label),
    EVERYONE(R.string.channel_add_permission_admin_members_label)
}

fun ChannelAddPermissionType.toDomainEnum(): ChannelAddPermission = when (this) {
    ChannelAddPermissionType.ADMINS -> ChannelAddPermission.ADMINS
    ChannelAddPermissionType.EVERYONE -> ChannelAddPermission.EVERYONE
}

fun ChannelAddPermission.toUiEnum(): ChannelAddPermissionType = when (this) {
    ChannelAddPermission.ADMINS -> ChannelAddPermissionType.ADMINS
    ChannelAddPermission.EVERYONE -> ChannelAddPermissionType.EVERYONE
}
