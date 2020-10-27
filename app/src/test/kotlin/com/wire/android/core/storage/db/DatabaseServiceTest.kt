package com.wire.android.core.storage.db

import android.database.sqlite.SQLiteException
import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.NoEntityFound
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DatabaseServiceTest : UnitTest() {

    private lateinit var databaseCall: suspend () -> TestEntity?

    private lateinit var databaseService: DatabaseService

    @Before
    fun setUp() {
        databaseService = object : DatabaseService {}
    }

    @Test
    fun `given request is called, when call returns non-null value, then returns the call's result regardless of the default value`() {
        databaseCall = { TEST_CALL_ENTITY }
        val defaultEntity = mockk<TestEntity>()

        runBlockingTest {
            databaseService.request(default = defaultEntity, call = databaseCall) shouldSucceed {
                it shouldBe TEST_CALL_ENTITY
            }
            verify { defaultEntity wasNot Called }
        }
    }

    @Test
    fun `given request is called, when call returns null value and there is a default value, then returns the default value`() {
        databaseCall = { null }

        runBlockingTest {
            databaseService.request(default = TEST_DEFAULT_ENTITY, call = databaseCall) shouldSucceed {
                it shouldBe TEST_DEFAULT_ENTITY
            }
        }
    }

    @Test
    fun `given request is called, when call returns null value and there is no default value, then returns NoEntityFound error`() {
        databaseCall = { null }

        runBlockingTest {
            databaseService.request(default = null, call = databaseCall) shouldFail {
                it shouldBe NoEntityFound
            }
        }
    }

    @Test
    fun `given request is called, when call throws SQLiteException, then returns SQLiteFailure with thrown exception`() {
        val exception = SQLiteException("Error!!")
        databaseCall = { throw exception }

        runBlockingTest {
            databaseService.request(call = databaseCall) shouldFail {
                it shouldEqual SQLiteFailure(exception)
            }
        }
    }

    @Test
    fun `given request is called, when call throws general Exception, then returns DatabaseFailure with thrown exception`() {
        val exception = Exception("Error!!")
        databaseCall = { throw exception }

        runBlockingTest {
            databaseService.request(call = databaseCall) shouldFail {
                it shouldEqual DatabaseFailure(exception)
            }
        }
    }

    private data class TestEntity(val name: String)

    companion object {
        private val TEST_CALL_ENTITY = TestEntity("testName")
        private val TEST_DEFAULT_ENTITY = TestEntity("defaultTest")
    }
}
