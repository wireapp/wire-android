package com.wire.android.migration.userDatabase

import com.wire.android.migration.util.orNullIfNegative
import com.wire.kalium.logic.data.conversation.Conversation

data class ScalaConversationData(
    val remoteId: String,
    val domain: String?,
    val name: String?,
    val creatorId: String,
    val type: Int,
)

class ScalaConversationDAO(private val db: ScalaUserDatabase) {

    fun conversations(): List<Conversation> {
        val cursor = db.rawQuery("SELECT * from $TABLE_NAME", null)
        try {
            val domainIndex = cursor.getColumnIndex(COLUMN_DOMAIN).orNullIfNegative()
            val idIndex = cursor.getColumnIndex(COLUMN_ID)
            val nameIndex = cursor.getColumnIndex(COLUMN_NAME)

        } catch (exception: Exception) {

        }
        return emptyList()
    }

//    qualified_id TEXT AS QualifiedIDEntity NOT NULL PRIMARY KEY,
//    name TEXT,
//    type TEXT AS ConversationEntity.Type NOT NULL,
//    team_id TEXT,
//    mls_group_id TEXT,
//    mls_group_state TEXT AS ConversationEntity.GroupState NOT NULL, // established
//    mls_epoch INTEGER DEFAULT 0 NOT NULL,
//    mls_proposal_timer TEXT,
//    protocol TEXT AS ConversationEntity.Protocol NOT NULL,
//    muted_status TEXT AS ConversationEntity.MutedStatus DEFAULT "ALL_ALLOWED" NOT NULL,
//    muted_time INTEGER DEFAULT 0 NOT NULL,
//    creator_id TEXT NOT NULL,
//    last_modified_date TEXT NOT NULL,
//    last_notified_message_date TEXT,
//    last_read_date TEXT DEFAULT "1970-01-01T00:00:00.000Z" NOT NULL,
//    access_list TEXT AS List<ConversationEntity.Access> NOT NULL,
//    access_role_list TEXT AS List<ConversationEntity.AccessRole> NOT NULL,
//    mls_last_keying_material_update INTEGER DEFAULT 0 NOT NULL,
//    mls_cipher_suite TEXT AS ConversationEntity.CipherSuite NOT NULL

    companion object {
        const val TABLE_NAME = "Conversations"
        const val COLUMN_ID = "remote_id"
        const val COLUMN_DOMAIN = "domain"
        const val COLUMN_NAME = "name"
        const val COLUMN_CREATOR = "creator"
    }
}
