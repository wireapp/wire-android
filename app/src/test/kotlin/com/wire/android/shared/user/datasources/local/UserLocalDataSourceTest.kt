package com.wire.android.shared.user.datasources.local

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
class UserLocalDataSourceTest : UnitTest() {

    @Mock
    private lateinit var userDao: UserDao

    private lateinit var userLocalDataSource : UserLocalDataSource

    @Before
    fun setUp() {
        userLocalDataSource = UserLocalDataSource(userDao)
    }

    @Test
    fun `given saveUser is called, when dao insertion is successful, then returns success`() {
        runBlockingTest {
            `when`(userDao.insert(USER_ENTITY)).thenReturn(Unit)

            userLocalDataSource.saveUser(TEST_USER_ID).assertRight()
        }
    }

    @Test
    fun `given saveUser is called, when dao insertion fails, then returns failure`() {
        runBlockingTest {
            `when`(userDao.insert(USER_ENTITY)).thenThrow(RuntimeException())

            userLocalDataSource.saveUser(TEST_USER_ID).onSuccess { fail("Expected a failure") }
        }
    }

    companion object {
        private const val TEST_USER_ID = "3324flkdnvdf"
        private val USER_ENTITY = UserEntity(TEST_USER_ID)
    }
}
