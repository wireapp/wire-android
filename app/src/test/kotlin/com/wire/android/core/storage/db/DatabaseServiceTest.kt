package com.wire.android.core.storage.db

import android.database.sqlite.SQLiteException
import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.NoEntityFound
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

@ExperimentalCoroutinesApi
class DatabaseServiceTest : UnitTest() {

    private lateinit var databaseCall: suspend () -> TestEntity?

    private lateinit var databaseService: DatabaseService

    @Before
    fun setUp() {
        databaseService = object : DatabaseService {}
    }

    @Test
    fun `given request is called, when call returns non-null value, then returns the call's result regardless of the default value`() =
        runBlockingTest {
            databaseCall = { TEST_CALL_ENTITY }
            val defaultEntity = mock(TestEntity::class.java)

            databaseService.request(default = defaultEntity, call = databaseCall).assertRight {
                assertThat(it).isEqualTo(TEST_CALL_ENTITY)
            }
            verifyNoInteractions(defaultEntity)
        }

    @Test
    fun `given request is called, when call returns null value and there is a default value, then returns the default value`() =
        runBlockingTest {
            databaseCall = { null }

            databaseService.request(default = TEST_DEFAULT_ENTITY, call = databaseCall).assertRight {
                assertThat(it).isEqualTo(TEST_DEFAULT_ENTITY)
            }
        }

    @Test
    fun `given request is called, when call returns null value and there is no default value, then returns NoEntityFound error`() =
        runBlockingTest {
            databaseCall = { null }

            databaseService.request(default = null, call = databaseCall).assertLeft {
                assertThat(it).isEqualTo(NoEntityFound)
            }
        }

    @Test
    fun `given request is called, when call throws SQLiteException, then returns SQLiteFailure with thrown exception`() =
        runBlockingTest {
            val exception = SQLiteException("Error!!")
            databaseCall = { throw exception }

            databaseService.request(call = databaseCall).assertLeft {
                assertThat(it).isEqualTo(SQLiteFailure(exception))
            }
        }

    @Test
    fun `given request is called, when call throws general Exception, then returns DatabaseFailure with thrown exception`() =
        runBlockingTest {
            val exception = Exception("Error!!")
            databaseCall = { throw exception }

            databaseService.request(call = databaseCall).assertLeft {
                assertThat(it).isEqualTo(DatabaseFailure(exception))
            }
        }

    private data class TestEntity(val name: String)

    companion object {
        private val TEST_CALL_ENTITY = TestEntity("testName")
        private val TEST_DEFAULT_ENTITY = TestEntity("defaultTest")
    }
}
