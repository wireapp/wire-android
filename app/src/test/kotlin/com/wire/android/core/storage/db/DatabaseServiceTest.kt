package com.wire.android.core.storage.db

import android.database.sqlite.SQLiteException
import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DatabaseServiceTest : UnitTest() {

    private lateinit var databaseCall: suspend () -> String

    private lateinit var databaseService: DatabaseService

    @Before
    fun setUp() {
        databaseService = object : DatabaseService {}
    }

    @Test
    fun `given request is called, when call is successful, then returns the call's result`() =
        runBlockingTest {
            val result = "Success!!"
            databaseCall = { result }

            databaseService.request(databaseCall).assertRight {
                assertThat(it).isEqualTo(result)
            }
        }

    @Test
    fun `given request is called, when call throws SQLiteException, then returns SQLiteFailure with thrown exception`() =
        runBlockingTest {
            val exception = SQLiteException("Error!!")
            databaseCall = { throw exception }

            databaseService.request(databaseCall).assertLeft {
                assertThat(it).isEqualTo(SQLiteFailure(exception))
            }
        }

    @Test
    fun `given request is called, when call throws general Exception, then returns DatabaseFailure with thrown exception`() =
        runBlockingTest {
            val exception = Exception("Error!!")
            databaseCall = { throw exception }

            databaseService.request(databaseCall).assertLeft {
                assertThat(it).isEqualTo(DatabaseFailure(exception))
            }
        }
}
