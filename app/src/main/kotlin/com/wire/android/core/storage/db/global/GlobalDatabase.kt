package com.wire.android.core.storage.db.global

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wire.android.shared.activeusers.datasources.local.ActiveUserEntity
import com.wire.android.shared.activeusers.datasources.local.ActiveUsersDao

@Database(entities = [ActiveUserEntity::class], version = GlobalDatabase.VERSION)
abstract class GlobalDatabase : RoomDatabase() {

    abstract fun activeUsersDao(): ActiveUsersDao

    companion object {
        const val NAME = "Global.db"
        const val VERSION = 1
    }
}
