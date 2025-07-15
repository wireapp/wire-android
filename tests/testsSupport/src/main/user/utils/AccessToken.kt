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
package user.utils

import java.time.LocalDateTime
import java.util.logging.Logger
import logger.WireTestLogger

class AccessToken(
    val value: String,
    var type: String,
    expiresIn: Long
) {
    companion object {
        private val log: Logger = WireTestLogger.getLog(AccessToken::class.simpleName.orEmpty())
    }

    @Suppress("MagicNumber")
    private val expiresOnDate: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 15)

    fun isInvalid(): Boolean = (value.isEmpty() || type.isEmpty())

    fun isExpired(): Boolean {
        val isExpired = LocalDateTime.now().isAfter(expiresOnDate)
        if (isExpired) {
            log.fine("Access token is expired")
        }
        return isExpired
    }
}
