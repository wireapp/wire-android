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
import com.wire.kalium.logic.data.user.type.isFederated
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

@Suppress("MagicNumber")
object UsernameMapper {

    /**
     * Returns the username for the given [OtherUser].
     * The username is the handle if it exists, otherwise it is the handle@domain for federated users.
     * For temporary users, the username is the time left until the user expires.
     */
    fun fromOtherUser(otherUser: OtherUser): String = with(otherUser) {
        return when {
            userType.isFederated() -> handle?.ifNotEmpty { "$handle@${id.domain}" }.orEmpty()
            expiresAt != null -> fromExpirationToHandle(expiresAt!!)
            else -> handle.orEmpty()
        }
    }

    fun fromExpirationToHandle(expiresAt: Instant): String {
        val diff = expiresAt.minus(Clock.System.now())
        val diffInMinutes = diff.inWholeMinutes
        return when {
            diffInMinutes <= 0 -> 0.minutes.toString(DurationUnit.MINUTES)
            diffInMinutes in 1..59 -> diff.toString(DurationUnit.MINUTES)
            else -> diff.toString(DurationUnit.HOURS)
        }
    }
}
