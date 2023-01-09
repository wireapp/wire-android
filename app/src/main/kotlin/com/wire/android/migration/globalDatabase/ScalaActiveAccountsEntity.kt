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
