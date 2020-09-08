package com.wire.android.shared.activeusers.datasources.local

import com.wire.android.UnitTest
import com.wire.android.core.functional.onSuccess
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class ActiveUsersLocalDataSourceTest : UnitTest() {

    @Mock
    private lateinit var activeUsersDao: ActiveUsersDao

    private lateinit var activeUsersLocalDataSource : ActiveUsersLocalDataSource

    @Before
    fun setUp() {
        activeUsersLocalDataSource =
            ActiveUsersLocalDataSource(activeUsersDao)
    }

    @Test
    fun `given saveActiveUser is called, when dao insertion is successful, then returns success`() {
        runBlockingTest {
            `when`(activeUsersDao.insert(ACTIVE_USER_ENTITY)).thenReturn(Unit)

            activeUsersLocalDataSource.saveActiveUser(TEST_USER_ID).assertRight()
        }
    }

    @Test
    fun `given saveActiveUser is called, when dao insertion fails, then returns failure`() {
        runBlockingTest {
            `when`(activeUsersDao.insert(ACTIVE_USER_ENTITY)).thenThrow(RuntimeException())

            activeUsersLocalDataSource.saveActiveUser(TEST_USER_ID).onSuccess { fail("Expected a failure") }
        }
    }

    companion object {
        private const val TEST_USER_ID = "3324flkdnvdf"
        private val ACTIVE_USER_ENTITY =
            ActiveUserEntity(TEST_USER_ID)
    }
}
