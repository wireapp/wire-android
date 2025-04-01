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
package com.wire.android.ui.home.conversations.details.updatechannelaccess

import android.os.Parcelable
import com.wire.android.R
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import kotlinx.parcelize.Parcelize
import com.wire.android.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class UpdateChannelAccessArgs(
    val conversationId: String,
    val accessType: ChannelAccessType = ChannelAccessType.PRIVATE,
    val permissionType: ChannelAddPermissionType = ChannelAddPermissionType.ADMINS
) : Parcelable

@Parcelize
enum class ChannelPermissionType(val label: Int) : Parcelable {
    ADMINS(R.string.channel_permission_admin_label),
    ADMIN_AND_MEMBERS(R.string.channel_permission_admin_members_label)
}
