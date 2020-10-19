package com.wire.android.shared.session.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.eq
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.datasources.local.SessionEntity
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.datasources.remote.AccessTokenResponse
import com.wire.android.shared.session.datasources.remote.SessionRemoteDataSource
import com.wire.android.shared.session.mapper.SessionMapper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class SessionDataSourceTest : UnitTest() {

    @Mock
    private lateinit var sessionMapper: SessionMapper

    @Mock
    private lateinit var remoteDataSource: SessionRemoteDataSource

    @Mock
    private lateinit var localDataSource: SessionLocalDataSource

    @Mock
    private lateinit var session: Session

    @Mock
    private lateinit var sessionEntity: SessionEntity


    private lateinit var sessionDataSource: SessionDataSource

    @Before
    fun setUp() {
        sessionDataSource = SessionDataSource(remoteDataSource, localDataSource, sessionMapper)
    }

    @Test
    fun `given save is called for dormant session, then maps given session to current entity and calls localDataSource for saving`() {
        runBlocking {
            `when`(sessionMapper.toSessionEntity(session, false)).thenReturn(sessionEntity)

            sessionDataSource.save(session, false)

            verify(sessionMapper).toSessionEntity(eq(session), eq(false))
            verify(localDataSource).save(sessionEntity)
            verify(localDataSource, never()).setCurrentSessionToDormant()
        }
    }

    @Test
    fun `given save is called for dormant session, when localDataSource returns success, then returns success`() {
        runBlocking {
            `when`(sessionMapper.toSessionEntity(session, false)).thenReturn(sessionEntity)
            `when`(localDataSource.save(sessionEntity)).thenReturn(Either.Right(Unit))

            sessionDataSource.save(session, false).assertRight()

            verify(sessionMapper).toSessionEntity(eq(session), eq(false))
            verify(localDataSource, never()).setCurrentSessionToDormant()
        }
    }

    @Test
    fun `given save is called for dormant session, when localDataSource returns a failure, then propagates the failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(sessionMapper.toSessionEntity(session, false)).thenReturn(sessionEntity)
            `when`(localDataSource.save(sessionEntity)).thenReturn(Either.Left(failure))

            sessionDataSource.save(session, false).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(sessionMapper).toSessionEntity(eq(session), eq(false))
            verify(localDataSource, never()).setCurrentSessionToDormant()
        }
    }

    @Test
    fun `given save is called for current session, when localDS updates previous session & saves the current one, then returns success`() {
        runBlocking {
            `when`(localDataSource.setCurrentSessionToDormant()).thenReturn(Either.Right(Unit))
            `when`(sessionMapper.toSessionEntity(session, true)).thenReturn(sessionEntity)
            `when`(localDataSource.save(sessionEntity)).thenReturn(Either.Right(Unit))

            sessionDataSource.save(session, true).assertRight()

            verify(localDataSource).setCurrentSessionToDormant()
            verify(sessionMapper).toSessionEntity(eq(session), eq(true))
            verify(localDataSource).save(sessionEntity)
        }
    }

    @Test
    fun `given save is called for current session, when localDS fails to update previous session, then directly propagates the failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(localDataSource.setCurrentSessionToDormant()).thenReturn(Either.Left(failure))

            sessionDataSource.save(session, true).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(localDataSource).setCurrentSessionToDormant()
            verify(sessionMapper, never()).toSessionEntity(eq(session), anyBoolean())
            verify(localDataSource, never()).save(sessionEntity)
        }
    }

    @Test
    fun `given save is called for current session, when localDS updates previous session but fails to save, then propagates the failure`() {
        runBlocking {
            val failure = SQLiteFailure()
            `when`(localDataSource.setCurrentSessionToDormant()).thenReturn(Either.Right(Unit))
            `when`(sessionMapper.toSessionEntity(session, true)).thenReturn(sessionEntity)
            `when`(localDataSource.save(sessionEntity)).thenReturn(Either.Left(failure))

            sessionDataSource.save(session, true).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(localDataSource).setCurrentSessionToDormant()
            verify(sessionMapper).toSessionEntity(eq(session), eq(true))
            verify(localDataSource).save(sessionEntity)
        }
    }

    @Test
    fun `given currentSession is called, when localDataSource emits an entity, then maps the entity to Session and emits it`() {
        `when`(sessionMapper.fromSessionEntity(sessionEntity)).thenReturn(session)
        `when`(localDataSource.currentSession()).thenReturn(flowOf(sessionEntity))

        runBlocking {
            sessionDataSource.currentSession().collect {
                assertThat(it).isEqualTo(session)
                verify(sessionMapper).fromSessionEntity(sessionEntity)
            }
        }
    }

    @Test
    fun `given currentSession is called, when localDataSource emits null, then directly emits null`() {
        `when`(localDataSource.currentSession()).thenReturn(flow { emit(null) })

        runBlocking {
            sessionDataSource.currentSession().collect {
                assertThat(it).isEqualTo(null)
                verifyNoInteractions(sessionMapper)
            }
        }
    }

    @Test
    fun `given accessToken is called, when remoteDataSource is successful, then maps the response and returns session`() {
        runBlocking {
            val accessTokenResponse = mock(AccessTokenResponse::class.java)
            val refreshToken = "refresh-token-123"
            `when`(remoteDataSource.accessToken(refreshToken)).thenReturn(Either.Right(accessTokenResponse))
            `when`(sessionMapper.fromAccessTokenResponse(accessTokenResponse, refreshToken)).thenReturn(session)

            val result = sessionDataSource.accessToken(refreshToken)

            result.assertRight {
                assertThat(it).isEqualTo(session)
            }
            verify(remoteDataSource).accessToken(refreshToken)
            verify(sessionMapper).fromAccessTokenResponse(accessTokenResponse, refreshToken)
        }
    }

    @Test
    fun `given accessToken is called, when remoteDataSource fails, then directly propagates that failure`() {
        runBlocking {
            val refreshToken = "refresh-token-123"
            `when`(remoteDataSource.accessToken(refreshToken)).thenReturn(Either.Left(ServerError))

            val result = sessionDataSource.accessToken(refreshToken)

            result.assertLeft {
                assertThat(it).isEqualTo(ServerError)
            }
            verify(remoteDataSource).accessToken(refreshToken)
            verifyNoInteractions(sessionMapper)
        }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when localDataSource successfully returns true, then propagates the result`() {
        testDoesCurrentSessionExistSuccessCase(true)
    }

    @Test
    fun `given doesCurrentSessionExist is called, when localDataSource successfully returns false, then propagates the result`() {
        testDoesCurrentSessionExistSuccessCase(false)
    }

    private fun testDoesCurrentSessionExistSuccessCase(exists: Boolean) = runBlocking {
        `when`(localDataSource.doesCurrentSessionExist()).thenReturn(Either.Right(exists))

        sessionDataSource.doesCurrentSessionExist().assertRight {
            assertThat(it).isEqualTo(exists)
        }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when localDataSource returns a failure, then propagates the failure`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            `when`(localDataSource.doesCurrentSessionExist()).thenReturn(Either.Left(failure))

            sessionDataSource.doesCurrentSessionExist().assertLeft {
                assertThat(it).isEqualTo(failure)
            }
        }
    }
}
