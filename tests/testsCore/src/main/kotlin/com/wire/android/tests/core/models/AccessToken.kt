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
package com.wire.android.tests.core.models

import com.wire.android.tests.core.utils.ZetaLogger
import java.time.LocalDateTime
import java.util.Date
import java.util.logging.Logger


class AccessToken {

    private val log: Logger = ZetaLogger.getLog(AccessToken::class.simpleName)

    private var value: String? = null
    private var type: String? = null
    private var expiresOnDate: LocalDateTime? = null

    constructor(value: String?, type: String?, expiresIn: Long) {
        this.type = type
        this.value = value
        this.expiresOnDate = LocalDateTime.now().plusSeconds(expiresIn - 15)
    }

    fun getType(): String? {
        return type
    }

    fun getValue(): String? {
        return value
    }

    fun isInvalid(): Boolean {
        return (value == null || type == null)
    }

    fun isExpired(): Boolean {
        val isExpired = LocalDateTime.now().isAfter(expiresOnDate)
        if (isExpired) {
            log.fine("Access token is expired")
        }
        return isExpired
    }
}
