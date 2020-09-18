package com.wire.android.shared.user.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.functional.Either
import com.wire.android.eq
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.user.UserSession
import com.wire.android.shared.user.datasources.local.SessionEntity
import com.wire.android.shared.user.datasources.local.UserLocalDataSource
import com.wire.android.shared.user.mapper.UserSessionMapper
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class UserDataSourceTest : UnitTest() {

    @Mock
    private lateinit var localDataSource: UserLocalDataSource

    @Mock
    private lateinit var userSessionMapper: UserSessionMapper

    @Mock
    private lateinit var userSession: UserSession

    @Mock
    private lateinit var sessionEntity: SessionEntity

    private lateinit var userDataSource: UserDataSource

    @Before
    fun setUp() {
        userDataSource = UserDataSource(localDataSource, userSessionMapper)
    }

    @Test
    fun `given saveUser is called, when localDataSource returns success, then returns success`() {
        runBlocking {
            `when`(localDataSource.saveUser(TEST_USER_ID)).thenReturn(Either.Right(Unit))

            userDataSource.saveUser(TEST_USER_ID).assertRight()

            verify(localDataSource).saveUser(TEST_USER_ID)
        }
    }

    @Test
    fun `given saveUser is called, when localDataSource returns a failure, then returns that failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(localDataSource.saveUser(TEST_USER_ID)).thenReturn(Either.Left(failure))

            userDataSource.saveUser(TEST_USER_ID).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(localDataSource).saveUser(TEST_USER_ID)
        }
    }

    @Test
    fun `given saveCurrentSession is called, then maps given session to current entity and calls localDataSource`() {
        runBlocking {
            `when`(userSessionMapper.toSessionEntity(userSession, true)).thenReturn(sessionEntity)

            userDataSource.saveCurrentSession(userSession)

            verify(userSessionMapper).toSessionEntity(eq(userSession), eq(true))
            verify(localDataSource).saveSession(sessionEntity)
        }
    }

    @Test
    fun `given saveCurrentSession is called, when localDataSource returns success, then returns success`() {
        runBlocking {
            `when`(userSessionMapper.toSessionEntity(userSession, true)).thenReturn(sessionEntity)
            `when`(localDataSource.saveSession(sessionEntity)).thenReturn(Either.Right(Unit))

            userDataSource.saveCurrentSession(userSession).assertRight()
        }
    }

    @Test
    fun `given saveCurrentSession is called, when localDataSource returns a failure, then returns that failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(userSessionMapper.toSessionEntity(userSession, true)).thenReturn(sessionEntity)
            `when`(localDataSource.saveSession(sessionEntity)).thenReturn(Either.Left(failure))

            userDataSource.saveCurrentSession(userSession).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
        }
    }

    companion object {
        private const val TEST_USER_ID = "asd123fkgj"
    }
}
