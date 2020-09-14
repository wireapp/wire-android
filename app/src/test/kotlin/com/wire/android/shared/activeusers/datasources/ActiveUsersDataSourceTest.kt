package com.wire.android.shared.activeusers.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.activeusers.datasources.local.ActiveUsersLocalDataSource
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class ActiveUsersDataSourceTest : UnitTest() {

    @Mock
    private lateinit var localDataSource: ActiveUsersLocalDataSource

    private lateinit var activeUsersDataSource: ActiveUsersDataSource

    @Before
    fun setUp() {
        activeUsersDataSource = ActiveUsersDataSource(localDataSource)
    }

    @Test
    fun `given saveActiveUser is called, when localDataSource returns success, then returns success`() {
        runBlocking {
            `when`(localDataSource.saveActiveUser(TEST_USER_ID)).thenReturn(Either.Right(Unit))

            activeUsersDataSource.saveActiveUser(TEST_USER_ID).assertRight()

            verify(localDataSource).saveActiveUser(TEST_USER_ID)
        }
    }

    @Test
    fun `given saveActiveUser is called, when localDataSource returns a failure, then returns that failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(localDataSource.saveActiveUser(TEST_USER_ID)).thenReturn(Either.Left(failure))

            activeUsersDataSource.saveActiveUser(TEST_USER_ID).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(localDataSource).saveActiveUser(TEST_USER_ID)
        }
    }

    companion object {
        private const val TEST_USER_ID = "asd123fkgj"
    }
}
