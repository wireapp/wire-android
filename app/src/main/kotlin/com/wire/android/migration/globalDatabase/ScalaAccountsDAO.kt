package com.wire.android.migration.globalDatabase

import android.annotation.SuppressLint
import com.wire.android.migration.util.getStringOrNull
import com.wire.android.migration.util.orNullIfNegative
import java.sql.SQLException

class ScalaAccountsDAO(private val db: ScalaGlobalDatabase) {

    @SuppressLint("Recycle")
    fun activeAccounts(): List<ScalaActiveAccountsEntity> {
        val cursor = db.rawQuery("SELECT * from $ACTIVE_ACCOUNTS_TABLE_NAME", null)
        try {
            val domainIndex: Int? = cursor.getColumnIndex(COLUMN_DOMAIN).orNullIfNegative()
            val idIndex: Int = cursor.getColumnIndex(COLUMN_ID)
            val teamIdIndex: Int = cursor.getColumnIndex(COLUMN_TEAM_ID)
            val accessTokenIndex: Int = cursor.getColumnIndex(COLUMN_ACCESS_TOKEN)
            val refreshTokenIndex: Int = cursor.getColumnIndex(COLUMN_REFRESH_TOKEN)
            val pushTokenIndex: Int = cursor.getColumnIndex(COLUMN_NATIVE_PUSH_TOKEN)
            val ssoIdIndex: Int = cursor.getColumnIndex(COLUMN_SSO_ID)

            return if (cursor.moveToFirst()) {
                // accu is a list of all the accounts we have found so far
                val accumulator = mutableListOf<ScalaActiveAccountsEntity>()
                do {
                    accumulator += ScalaActiveAccountsEntity(
                        id = cursor.getString(idIndex),
                        domain = domainIndex?.let { cursor.getStringOrNull(domainIndex) },
                        teamId = cursor.getStringOrNull(teamIdIndex),
                        refreshToken = cursor.getString(refreshTokenIndex),
                        accessToken = ScalaAccessTokenEntity.fromString(cursor.getStringOrNull(accessTokenIndex)),
                        pushToken = cursor.getStringOrNull(pushTokenIndex),
                        ssoId = ScalaSsoIdEntity.fromString(cursor.getStringOrNull(ssoIdIndex))
                    )
                } while (cursor.moveToNext())
                accumulator
            } else {
                // cursor.moveToFirst() will return false if the cursor is empty
                emptyList()
            }
        } catch (e: SQLException) {
            // TODO: handle error with Either ?
            throw e
        } finally {
            cursor.close()
        }
    }

    private companion object {
        const val ACTIVE_ACCOUNTS_TABLE_NAME = "ActiveAccounts"
        const val COLUMN_DOMAIN = "domain"
        const val COLUMN_ID = "_id"
        const val COLUMN_TEAM_ID = "team_id"
        const val COLUMN_REFRESH_TOKEN = "cookie" // in scala app this can be an empty string
        const val COLUMN_ACCESS_TOKEN = "access_token"
        const val COLUMN_NATIVE_PUSH_TOKEN = "registered_push"
        const val COLUMN_SSO_ID = "sso_id"
    }
}
