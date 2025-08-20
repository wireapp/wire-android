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

import android.os.Parcelable
import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group.Channel.ChannelAccess
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ChannelAccessType(@StringRes val labelResId: Int) : Parcelable {
    PUBLIC(R.string.channel_public_label),
    PRIVATE(R.string.channel_private_label)
}

fun ChannelAccess.toUiEnum(): ChannelAccessType = when (this) {
    ChannelAccess.PUBLIC -> ChannelAccessType.PUBLIC
    ChannelAccess.PRIVATE -> ChannelAccessType.PRIVATE
}
