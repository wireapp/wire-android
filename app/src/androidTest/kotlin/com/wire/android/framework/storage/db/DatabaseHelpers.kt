package com.wire.android.framework.storage.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor

inline fun <reified T : RoomDatabase> buildDatabase(appContext: Context, dispatcher: CoroutineDispatcher): T =
    buildDatabase(T::class.java, appContext, dispatcher)

fun <T : RoomDatabase> buildDatabase(clazz: Class<T>, appContext: Context, dispatcher: CoroutineDispatcher): T =
    Room.inMemoryDatabaseBuilder(appContext, clazz)
        .setQueryExecutor(dispatcher.asExecutor())
        .setTransactionExecutor(dispatcher.asExecutor())
        .build()

fun RoomDatabase.clearTestData() = with(this) {
    clearAllTables()
    close()
}
