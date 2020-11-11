package com.wire.android.core.storage.db.di

import androidx.room.Room
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.core.storage.db.user.UserDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { Room.databaseBuilder(androidContext(), GlobalDatabase::class.java, GlobalDatabase.NAME).build() }

    //TODO: create separate database instances for each user!!!
    single { Room.databaseBuilder(androidContext(), UserDatabase::class.java, "DummyUserDatabase").build() }
}
