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

package com.wire.android.ui.userprofile.common

import com.wire.android.util.ifNotEmpty
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.type.UserType

object UsernameMapper {

    /**
     * Returns the username for the given [OtherUser].
     * The username is the handle if it exists, otherwise it is the handle@domain for federated users.
     */
    fun fromOtherUser(otherUser: OtherUser): String = with(otherUser) {
        val userId = otherUser.id
        return when (otherUser.userType) {
            UserType.FEDERATED -> handle?.ifNotEmpty { "$handle@${userId.domain}" }.orEmpty()
            else -> handle.orEmpty()
        }
    }
}
