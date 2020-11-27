package com.wire.android.core.storage.db.user

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wire.android.feature.contact.datasources.local.ContactDao
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.conversation.list.datasources.local.ConversationDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity

private const val VERSION = 1

@Database(entities = [ConversationEntity::class, ContactEntity::class], version = VERSION)
abstract class UserDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    abstract fun contactDao(): ContactDao
}
