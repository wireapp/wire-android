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

    @Mock
    private lateinit var userEntity: UserEntity

    private lateinit var userLocalDataSource: UserLocalDataSource

    @Before
    fun setUp() {
        userLocalDataSource = UserLocalDataSource(userDao)
    }

    @Test
    fun `given save is called, when dao insertion is successful, then returns success`() {
        runBlockingTest {
            `when`(userDao.insert(userEntity)).thenReturn(Unit)

            userLocalDataSource.save(userEntity).assertRight()
        }
    }

    @Test
    fun `given save is called, when dao insertion fails, then returns failure`() {
        runBlockingTest {
            `when`(userDao.insert(userEntity)).thenThrow(RuntimeException())

            userLocalDataSource.save(userEntity).onSuccess { fail("Expected a failure") }
        }
    }
}
