package com.wire.android.shared.activeuser.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.activeuser.datasources.local.ActiveUserLocalDataSource
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class ActiveUserDataSourceTest : UnitTest() {

    @Mock
    private lateinit var localDataSource: ActiveUserLocalDataSource

    private lateinit var activeUserDataSource: ActiveUserDataSource

    @Before
    fun setUp() {
        activeUserDataSource = ActiveUserDataSource(localDataSource)
    }

    @Test
    fun `given saveActiveUser is called, when localDataSource returns success, then returns success`() {
        runBlocking {
            `when`(localDataSource.saveActiveUser(TEST_USER_ID)).thenReturn(Either.Right(Unit))

            activeUserDataSource.saveActiveUser(TEST_USER_ID).assertRight()

            verify(localDataSource).saveActiveUser(TEST_USER_ID)
        }
    }

    @Test
    fun `given saveActiveUser is called, when localDataSource returns a failure, then returns that failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(localDataSource.saveActiveUser(TEST_USER_ID)).thenReturn(Either.Left(failure))

            activeUserDataSource.saveActiveUser(TEST_USER_ID).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(localDataSource).saveActiveUser(TEST_USER_ID)
        }
    }

    @Test
    fun `given hasActiveUser is called, when localDataSource returns null user id, then returns false`() {
        `when`(localDataSource.activeUserId()).thenReturn(null)

        assertThat(activeUserDataSource.hasActiveUser()).isFalse()
    }

    @Test
    fun `given hasActiveUser is called, when localDataSource returns a user id, then returns true`() {
        `when`(localDataSource.activeUserId()).thenReturn(TEST_USER_ID)

        assertThat(activeUserDataSource.hasActiveUser()).isTrue()
    }

    companion object {
        private const val TEST_USER_ID = "asd123fkgj"
    }
}
