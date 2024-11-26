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

package com.wire.android.ui.home.conversationslist.model

import kotlinx.serialization.Serializable

@Serializable
sealed class BadgeEventType {

    @Serializable
    data class UnreadMessage(val unreadMessageCount: Int) : BadgeEventType()

    @Serializable
    data object UnreadMention : BadgeEventType()

    @Serializable
    data object UnreadReply : BadgeEventType()

    @Serializable
    data object MissedCall : BadgeEventType()

    @Serializable
    data object Knock : BadgeEventType()

    @Serializable
    data object ReceivedConnectionRequest : BadgeEventType()

    @Serializable
    data object SentConnectRequest : BadgeEventType()

    @Serializable
    data object Blocked : BadgeEventType()

    @Serializable
    data object Deleted : BadgeEventType()

    @Serializable
    data object None : BadgeEventType()
}
