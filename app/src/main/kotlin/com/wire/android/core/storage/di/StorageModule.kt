package com.wire.android.core.storage.di

import androidx.room.Room
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.core.storage.preferences.GlobalPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val storageModule = module {
    single { Room.databaseBuilder(androidContext(), GlobalDatabase::class.java, GlobalDatabase.NAME).build() }

    single { GlobalPreferences(androidContext()) }
}
