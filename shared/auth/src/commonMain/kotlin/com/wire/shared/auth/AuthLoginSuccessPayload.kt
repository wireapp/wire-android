/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.shared.auth

data class AuthLoginSuccessPayload(
    val userIdValue: String,
    val userIdDomain: String?,
    val accessTokenValue: String,
    val accessTokenType: String,
    val accessTokenExpiresInSeconds: Int?,
    val refreshTokenValue: String,
    val refreshTokenCookieName: String = REFRESH_TOKEN_COOKIE_NAME,
    val refreshTokenCookieDomain: String?,
    val refreshTokenCookiePath: String = REFRESH_TOKEN_COOKIE_PATH,
    val refreshTokenCookieSecure: Boolean = true,
    val refreshTokenCookieHttpOnly: Boolean = true,
    val email: String?,
    val password: String?,
    val secondFactorCode: String?,
    val initialSyncCompleted: Boolean,
    val isE2EIRequired: Boolean,
    val clientId: String?,
) {
    companion object {
        const val REFRESH_TOKEN_COOKIE_NAME = "zuid"
        const val REFRESH_TOKEN_COOKIE_PATH = "/"
    }
}
