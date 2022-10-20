package com.wire.android.migration.userDatabase

import com.wire.android.appLogger
import com.wire.android.migration.util.getStringOrNull
import com.wire.android.migration.util.orNullIfNegative

data class ScalaConversationData(
    val remoteId: String,
    val domain: String?,
    val name: String?,
    val creatorId: String,
    val type: Int,
)

class ScalaConversationDAO(private val db: ScalaUserDatabase) {

    fun conversations(): List<ScalaConversationData> {
        val cursor = db.rawQuery("SELECT * from $TABLE_NAME", null)
        return try {
            val domainIndex = cursor.getColumnIndex(COLUMN_DOMAIN).orNullIfNegative()
            val idIndex = cursor.getColumnIndex(COLUMN_ID)
            val nameIndex = cursor.getColumnIndex(COLUMN_NAME)
            val creatorIdIndex = cursor.getColumnIndex(COLUMN_CREATOR)
            val typeIndex = cursor.getColumnIndex(COLUMN_TYPE)
            if (!cursor.moveToFirst()) {
                emptyList()
            } else {
                val accumulator = mutableListOf<ScalaConversationData>()
                do {
                    accumulator += ScalaConversationData(
                        remoteId = cursor.getStringOrNull(idIndex).orEmpty(),
                        domain = domainIndex?.let { cursor.getStringOrNull(domainIndex) },
                        name = cursor.getStringOrNull(nameIndex),
                        creatorId = cursor.getString(creatorIdIndex),
                        type = cursor.getInt(typeIndex)
                    )
                } while (cursor.moveToNext())
                accumulator
            }
        } catch (exception: Exception) {
            appLogger.e("Error while querying old conversations $exception")
            emptyList()
        }
    }

    companion object {
        const val TABLE_NAME = "Conversations"
        const val COLUMN_ID = "remote_id"
        const val COLUMN_DOMAIN = "domain"
        const val COLUMN_NAME = "name"
        const val COLUMN_CREATOR = "creator"
        const val COLUMN_TYPE = "conv_type"
    }
}
