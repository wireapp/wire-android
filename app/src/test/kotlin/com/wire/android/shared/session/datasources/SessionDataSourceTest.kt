package com.wire.android.shared.session.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either
import com.wire.android.eq
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.datasources.local.SessionEntity
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.mapper.SessionMapper
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
    private lateinit var localDataSource: SessionLocalDataSource

    @Mock
    private lateinit var session: Session

    @Mock
    private lateinit var sessionEntity: SessionEntity


    private lateinit var sessionDataSource: SessionDataSource

    @Before
    fun setUp() {
        sessionDataSource = SessionDataSource(localDataSource, sessionMapper)
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
}
