package com.wire.android.shared.session.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

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
    fun `given save is called, then maps given session to current entity and calls localDataSource`() {
        runBlocking {
            `when`(sessionMapper.toSessionEntity(session, true)).thenReturn(sessionEntity)

            sessionDataSource.save(session)

            verify(sessionMapper).toSessionEntity(eq(session), eq(true))
            verify(localDataSource).saveSession(sessionEntity)
        }
    }

    @Test
    fun `given save is called, when localDataSource returns success, then returns success`() {
        runBlocking {
            `when`(sessionMapper.toSessionEntity(session, true)).thenReturn(sessionEntity)
            `when`(localDataSource.saveSession(sessionEntity)).thenReturn(Either.Right(Unit))

            sessionDataSource.save(session).assertRight()
        }
    }

    @Test
    fun `given save is called, when localDataSource returns a failure, then returns that failure`() {
        runBlocking {
            val failure = DatabaseFailure()
            `when`(sessionMapper.toSessionEntity(session, true)).thenReturn(sessionEntity)
            `when`(localDataSource.saveSession(sessionEntity)).thenReturn(Either.Left(failure))

            sessionDataSource.save(session).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
        }
    }
}
