/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.selfdeletion

import com.wire.android.ui.home.messagecomposer.state.SelfDeletionDuration

object SelfDeletionMapper {
    fun Int?.toSelfDeletionDuration(): SelfDeletionDuration =
        when (this) {
            in 1..10 -> SelfDeletionDuration.TenSeconds
            in 11..300 -> SelfDeletionDuration.FiveMinutes
            in 301..3600 -> SelfDeletionDuration.OneHour
            in 3601..86400 -> SelfDeletionDuration.OneDay
            in 86401..604800 -> SelfDeletionDuration.OneWeek
            in 604801..2592000 -> SelfDeletionDuration.FourWeeks
            else -> SelfDeletionDuration.None
        }
}
