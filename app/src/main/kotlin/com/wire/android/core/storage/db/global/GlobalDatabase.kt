package com.wire.android.core.storage.db.global

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wire.android.shared.activeuser.datasources.local.ActiveUserDao
import com.wire.android.shared.activeuser.datasources.local.ActiveUserEntity

@Database(entities = [ActiveUserEntity::class], version = GlobalDatabase.VERSION)
abstract class GlobalDatabase : RoomDatabase() {

    abstract fun activeUserDao(): ActiveUserDao

    companion object {
        const val NAME = "Global.db"
        const val VERSION = 1
    }
}
