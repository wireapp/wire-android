package com.wire.android.framework.storage.db

import android.content.Context
import androidx.room.RoomDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class DatabaseTestRule<D : RoomDatabase>(private val appContext: Context, private val clazz: Class<D>) : TestWatcher() {

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    lateinit var database: D

    override fun starting(description: Description?) {
        super.starting(description)
        database = buildDatabase(clazz, appContext, testDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        database.clearTestData()
    }

    fun runTest(test: suspend TestCoroutineScope.() -> Unit) = testScope.runBlockingTest(test)

    companion object {
        inline fun <reified D : RoomDatabase> create(appContext: Context) =
            DatabaseTestRule(appContext, D::class.java)
    }
}
