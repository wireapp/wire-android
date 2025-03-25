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

import com.wire.android.R
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group.Channel.ChannelPermission

enum class ChannelPermissionType(val label: Int) {
    ADMINS(R.string.channel_permission_admin_label),
    ADMIN_AND_MEMBERS(R.string.channel_permission_admin_members_label)
}

fun ChannelPermissionType.toDomainEnum(): ChannelPermission = when (this) {
    ChannelPermissionType.ADMINS -> ChannelPermission.ADMINS
    ChannelPermissionType.ADMIN_AND_MEMBERS -> ChannelPermission.ADMINS_AND_MEMBERS
}

fun ChannelPermission.toUiEnum(): ChannelPermissionType = when (this) {
    ChannelPermission.ADMINS -> ChannelPermissionType.ADMINS
    ChannelPermission.ADMINS_AND_MEMBERS -> ChannelPermissionType.ADMIN_AND_MEMBERS
}
