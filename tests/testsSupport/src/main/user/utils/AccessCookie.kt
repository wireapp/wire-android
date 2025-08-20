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

import java.net.HttpCookie
import java.util.Date

class AccessCookie(
    val name: String,
    private val expirationDate: Date,
    val value: String
) {

    fun isExpired(): Boolean = Date().after(expirationDate)

    @Suppress("MagicNumber")
    constructor(cookieName: String, cookies: List<HttpCookie>) : this(
        cookieName,
        Date(Date().time + (cookies.first { it.name == cookieName }.maxAge * 1000)),
        cookies.first { it.name == cookieName }.value
    )
}
