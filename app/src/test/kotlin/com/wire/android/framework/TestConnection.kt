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

package com.wire.android.framework

import com.wire.kalium.logic.data.user.Connection
import com.wire.kalium.logic.data.user.ConnectionState
import kotlin.time.Instant

object TestConnection {
    val CONNECTION = Connection(
        TestConversation.ID.value,
        "FROM",
        Instant.parse("2022-03-30T15:36:00.000Z"),
        TestConversation.ID,
        TestUser.USER_ID,
        ConnectionState.SENT,
        TestUser.OTHER_USER.id.value,
        TestUser.OTHER_USER
    )
}
