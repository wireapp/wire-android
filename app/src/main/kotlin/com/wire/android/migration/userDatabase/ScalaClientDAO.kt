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

package com.wire.android.migration.userDatabase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.sql.SQLException

data class ScalaClientInfo(
    val clientId: String,
    val otrLastPrekeyId: Int
)

class ScalaClientDAO(
    private val db: ScalaUserDatabase,
    private val coroutineDispatcher: CoroutineDispatcher
) {
    suspend fun clientInfo(): ScalaClientInfo? = withContext(coroutineDispatcher) {
        val clientIdCursor =
            db.rawQuery(
                "SELECT $VALUE_COLUMN_NAME FROM $KEY_VALUES_TABLE_NAME WHERE $KEY_COLUMN_NAME = ?",
                arrayOf(SELF_CLIENT_KEY)
            )
        val otrLastPrekeyIdCursor =
            db.rawQuery(
                "SELECT $VALUE_COLUMN_NAME FROM $KEY_VALUES_TABLE_NAME WHERE $KEY_COLUMN_NAME = ?",
                arrayOf(OTR_LAT_PREKEY_ID)
            )

        return@withContext try {
            if (clientIdCursor.moveToFirst().not() or otrLastPrekeyIdCursor.moveToFirst().not()) {
                return@withContext null
            }
            val clientId = clientIdCursor.getString(0)
            val otrLastPrekeyId = otrLastPrekeyIdCursor.getString(0).toInt()

            ScalaClientInfo(clientId, otrLastPrekeyId)
        } catch (e: SQLException) {
            null
        } finally {
            clientIdCursor.close()
            otrLastPrekeyIdCursor.close()
        }
    }

    private companion object {
        const val KEY_VALUES_TABLE_NAME = "KeyValues"
        const val KEY_COLUMN_NAME = "key"
        const val VALUE_COLUMN_NAME = "value"
        const val SELF_CLIENT_KEY = "self_client"
        const val OTR_LAT_PREKEY_ID = "otr_last_prekey_id"
    }
}
