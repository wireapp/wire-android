package com.wire.android.core.storage.db.user

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wire.android.feature.contact.datasources.local.ContactDao
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.conversation.list.datasources.local.ConversationDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMembersDao

@Database(entities = [ConversationEntity::class, ContactEntity::class, ConversationMemberEntity::class], version = UserDatabase.VERSION)
abstract class UserDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    abstract fun contactDao(): ContactDao

    abstract fun conversationMembersDao(): ConversationMembersDao

    companion object {
        const val VERSION = 1
    }
}
