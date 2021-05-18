package com.wire.android.core.storage.db.user

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wire.android.feature.auth.client.datasource.local.ClientDao
import com.wire.android.feature.auth.client.datasource.local.ClientEntity
import com.wire.android.feature.contact.datasources.local.ContactDao
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.list.datasources.local.ConversationListDao
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMembersDao
import com.wire.android.shared.asset.datasources.local.AssetDao
import com.wire.android.shared.asset.datasources.local.AssetEntity

@Database(
    entities = [ConversationEntity::class, ContactEntity::class, ConversationMemberEntity::class, AssetEntity::class, ClientEntity::class],
    version = UserDatabase.VERSION
)
abstract class UserDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    abstract fun contactDao(): ContactDao

    abstract fun conversationMembersDao(): ConversationMembersDao

    abstract fun conversationListDao(): ConversationListDao

    abstract fun assetDao(): AssetDao

    abstract fun clientDao(): ClientDao

    companion object {
        const val VERSION = 1
    }
}
