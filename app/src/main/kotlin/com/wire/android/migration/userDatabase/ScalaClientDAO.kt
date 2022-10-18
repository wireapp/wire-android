package com.wire.android.migration.userDatabase

import java.sql.SQLException

data class ScalaClientInfo(
    val clientId: String,
    val otrLastPrekeyId: Int
)

class ScalaClientDAO(private val db: ScalaUserDatabase) {
    fun clientInfo(): ScalaClientInfo? {
        val clientIdCursor =
            db.rawQuery(
                "SELECT $VALUE_COLUMN_NAME FROM $KEY_VALUES_TABLE_NAME WHERE ? = ?",
                arrayOf(KEY_COLUMN_NAME, SELF_CLIENT_KEY)
            )
        val otrLastPrekeyIdCursor =
            db.rawQuery(
                "SELECT $VALUE_COLUMN_NAME FROM $KEY_VALUES_TABLE_NAME WHERE ? = ?",
                arrayOf(KEY_COLUMN_NAME, OTR_LAT_PREKEY_ID)
            )

        return try {
            if (clientIdCursor.moveToFirst().not() or otrLastPrekeyIdCursor.moveToFirst().not()) {
                return null
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
