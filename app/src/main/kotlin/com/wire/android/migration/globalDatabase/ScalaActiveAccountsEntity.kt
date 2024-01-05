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

package com.wire.android.migration.globalDatabase

import org.json.JSONException
import org.json.JSONObject

data class ScalaSsoIdEntity(
    val subject: String,
    val tenant: String
) {
    companion object {
        fun fromString(tokenString: String?): ScalaSsoIdEntity? =
            try {
                tokenString?.let {
                    val json = JSONObject(it)
                    ScalaSsoIdEntity(
                        subject = json.getString(KEY_SUBJECT),
                        tenant = json.getString(KEY_TENANT)
                    )
                }
            } catch (e: JSONException) {
                null
            }

        private const val KEY_SUBJECT = "subject"
        private const val KEY_TENANT = "tenant"
    }
}

data class ScalaAccessTokenEntity(
    val token: String,
    val tokenType: String,
    val expiresInMillis: Long
) {
    companion object {
        fun fromString(tokenString: String?): ScalaAccessTokenEntity? = tokenString?.let {
            try {
                val json = JSONObject(it)
                ScalaAccessTokenEntity(
                    token = json.getString(KEY_TOKEN),
                    tokenType = json.getString(KEY_TOKEN_TYPE),
                    expiresInMillis = json.getLong(KEY_EXPIRY)
                )
            } catch (e: JSONException) {
                null
            }
        }

        private const val KEY_TOKEN = "token"
        private const val KEY_TOKEN_TYPE = "type"
        private const val KEY_EXPIRY = "expires"
    }
}

data class ScalaActiveAccountsEntity(
    val id: String,
    val domain: String?,
    val teamId: String?,
    val refreshToken: String,
    val accessToken: ScalaAccessTokenEntity?,
    val pushToken: String?,
    val ssoId: ScalaSsoIdEntity?
)
