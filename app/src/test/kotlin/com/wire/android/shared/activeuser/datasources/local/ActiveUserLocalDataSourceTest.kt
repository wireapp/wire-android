package com.wire.android.shared.activeuser.datasources.local

import com.wire.android.UnitTest
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.storage.preferences.GlobalPreferences
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class ActiveUserLocalDataSourceTest : UnitTest() {

    @Mock
    private lateinit var activeUserDao: ActiveUserDao

    @Mock
    private lateinit var globalPreferences: GlobalPreferences

    private lateinit var activeUserLocalDataSource : ActiveUserLocalDataSource

    @Before
    fun setUp() {
        activeUserLocalDataSource = ActiveUserLocalDataSource(activeUserDao, globalPreferences)
    }

    @Test
    fun `given saveActiveUser is called, when dao insertion is successful, then updates active user id pref and returns success`() {
        runBlockingTest {
            `when`(activeUserDao.insert(ACTIVE_USER_ENTITY)).thenReturn(Unit)

            activeUserLocalDataSource.saveActiveUser(TEST_USER_ID).assertRight()
            verify(globalPreferences).activeUserId = TEST_USER_ID
        }
    }

    @Test
    fun `given saveActiveUser is called, when dao insertion fails, then returns failure`() {
        runBlockingTest {
            `when`(activeUserDao.insert(ACTIVE_USER_ENTITY)).thenThrow(RuntimeException())

            activeUserLocalDataSource.saveActiveUser(TEST_USER_ID).onSuccess { fail("Expected a failure") }
        }
    }

    companion object {
        private const val TEST_USER_ID = "3324flkdnvdf"
        private val ACTIVE_USER_ENTITY = ActiveUserEntity(TEST_USER_ID)
    }
}
