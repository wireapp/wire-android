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

import java.net.HttpCookie
import java.util.Date

class AccessCookie {

    private var name: String? = null
    private var expirationDate: Date? = null
    private var value: String? = null

    fun getName(): String? {
        return name
    }

    fun getValue(): String? {
        return value
    }

    fun isExpired(): Boolean {
        return Date().after(expirationDate)
    }

    constructor(cookieName: String?, cookies: List<HttpCookie>) {
        if (!cookies.stream().anyMatch { x: HttpCookie -> x.name == "zuid" }) {
            throw RuntimeException(String.format("No cookie found with name '%s'", cookieName))
        }
        val newCookie = cookies.stream().filter { x: HttpCookie -> x.name == cookieName }.findFirst().get()
        this.name = cookieName
        if (newCookie != null) {
            this.value = newCookie.value
        }
        if (newCookie != null) {
            val date = Date()
            this.expirationDate = Date(date.time + (newCookie.maxAge * 1000))
        }
    }

    fun AccessCookie(cookieName: String, cookies: List<HttpCookie>) {
        val newCookie = cookies.stream().filter { x: HttpCookie -> x.name == cookieName }.findFirst().get()
        this.name = cookieName
        this.value = newCookie.value
        val now: Date = Date()
        this.expirationDate = Date(now.getTime() + (newCookie.maxAge * 1000))
    }
}
