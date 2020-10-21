package com.wire.android.core.storage.db.user

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wire.android.feature.conversation.list.datasources.local.ConversationDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity

private const val VERSION = 1

@Database(entities = [ConversationEntity::class], version = VERSION)
abstract class UserDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
}
