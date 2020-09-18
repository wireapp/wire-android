package com.wire.android.core.storage.db.global

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wire.android.shared.user.datasources.local.UserDao
import com.wire.android.shared.user.datasources.local.UserEntity

@Database(entities = [UserEntity::class], version = GlobalDatabase.VERSION)
abstract class GlobalDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "Global.db"
        const val VERSION = 1
    }
}
