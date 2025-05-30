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

import java.util.Base64
class BasicAuth {

    var user: String? = null
        private set

    var password: String? = null
        private set

    private var encoded: String

    constructor(user: String, password: String) {
        this.user = user
        this.password = password
        encoded = try {
            Base64.getEncoder().encodeToString("$user:$password".toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            throw RuntimeException("Could not compile basic auth credentials for backend", e)
        }
    }

    constructor(encoded: String) {
        this.encoded = encoded
    }


    fun getEncoded(): String = "Basic $encoded"
}
