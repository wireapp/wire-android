package com.wire.android.core.storage.db

import androidx.room.Room
import androidx.room.RoomDatabase
import com.wire.android.InstrumentationTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest

@ExperimentalCoroutinesApi
abstract class DatabaseTest : InstrumentationTest() {

    @PublishedApi
    internal val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    protected inline fun <reified T : RoomDatabase> buildDatabase(): T =
        Room.inMemoryDatabaseBuilder(appContext, T::class.java)
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .build()

    fun runTest(test: suspend TestCoroutineScope.() -> Unit) = testScope.runBlockingTest(test)
}
