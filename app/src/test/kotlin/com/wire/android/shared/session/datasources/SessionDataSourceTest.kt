package com.wire.android.shared.session.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.datasources.local.SessionEntity
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.datasources.remote.AccessTokenResponse
import com.wire.android.shared.session.datasources.remote.SessionRemoteDataSource
import com.wire.android.shared.session.mapper.SessionMapper
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class SessionDataSourceTest : UnitTest() {

    @MockK
    private lateinit var sessionMapper: SessionMapper

    @MockK
    private lateinit var remoteDataSource: SessionRemoteDataSource

    @MockK
    private lateinit var localDataSource: SessionLocalDataSource

    @MockK
    private lateinit var session: Session

    @MockK
    private lateinit var sessionEntity: SessionEntity


    private lateinit var sessionDataSource: SessionDataSource

    @Before
    fun setUp() {
        sessionDataSource = SessionDataSource(remoteDataSource, localDataSource, sessionMapper)
    }

    @Test
    fun `given save is called for dormant session, then maps given session to current entity and calls localDataSource for saving`() {
        every { sessionMapper.toSessionEntity(session, false) } returns sessionEntity
        coEvery { localDataSource.save(sessionEntity) } returns Either.Right(Unit)

        runBlocking { sessionDataSource.save(session, false) }

        verify(exactly = 1) { sessionMapper.toSessionEntity(session, false) }
        coVerify(exactly = 1) { localDataSource.save(sessionEntity) }
        coVerify(inverse = true) { localDataSource.setCurrentSessionToDormant() }
    }

    @Test
    fun `given save is called for dormant session, when localDataSource returns success, then returns success`() {
        every { sessionMapper.toSessionEntity(session, false) } returns sessionEntity
        coEvery { localDataSource.save(sessionEntity) } returns Either.Right(Unit)

        val result = runBlocking { sessionDataSource.save(session, false) }

        result shouldSucceed { it shouldBe Unit }
        verify(exactly = 1) { sessionMapper.toSessionEntity(session, false) }
        coVerify(inverse = true) { localDataSource.setCurrentSessionToDormant() }
    }

    @Test
    fun `given save is called for dormant session, when localDataSource returns a failure, then propagates the failure`() {
        val failure = DatabaseFailure()
        every { sessionMapper.toSessionEntity(session, false) } returns sessionEntity
        coEvery { localDataSource.save(sessionEntity) } returns Either.Left(failure)

        val result = runBlocking { sessionDataSource.save(session, false) }

        result shouldFail { it shouldBe failure }
        verify(exactly = 1) { sessionMapper.toSessionEntity(session, false) }
        coVerify(inverse = true) { localDataSource.setCurrentSessionToDormant() }
    }

    @Test
    fun `given save is called for current session, when localDS updates previous session & saves the current one, then returns success`() {
        coEvery { localDataSource.setCurrentSessionToDormant() } returns Either.Right(Unit)
        every { sessionMapper.toSessionEntity(session, true) } returns sessionEntity
        coEvery { localDataSource.save(sessionEntity) } returns Either.Right(Unit)

        val result = runBlocking { sessionDataSource.save(session, true) }

        result shouldSucceed { it shouldBe Unit }
        coVerify(exactly = 1) { localDataSource.setCurrentSessionToDormant() }
        verify(exactly = 1) { sessionMapper.toSessionEntity(session, true) }
        coVerify(exactly = 1) { localDataSource.save(sessionEntity) }
    }

    @Test
    fun `given save is called for current session, when localDS fails to update previous session, then directly propagates the failure`() {
        val failure = DatabaseFailure()
        coEvery { localDataSource.setCurrentSessionToDormant() } returns Either.Left(failure)

        val result = runBlocking { sessionDataSource.save(session, true) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { localDataSource.setCurrentSessionToDormant() }
        verify(inverse = true) { sessionMapper.toSessionEntity(session, any()) }
        coVerify(inverse = true) { localDataSource.save(sessionEntity) }
    }

    @Test
    fun `given save is called for current session, when localDS updates previous session but fails to save, then propagates the failure`() {
        val failure = SQLiteFailure()
        coEvery { localDataSource.setCurrentSessionToDormant() } returns Either.Right(Unit)
        every { sessionMapper.toSessionEntity(session, true) } returns sessionEntity
        coEvery { localDataSource.save(sessionEntity) } returns Either.Left(failure)

        val result = runBlocking { sessionDataSource.save(session, true) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { localDataSource.setCurrentSessionToDormant() }
        verify(exactly = 1) { sessionMapper.toSessionEntity(session, true) }
        coVerify(exactly = 1) { localDataSource.save(sessionEntity) }
    }

    @Test
    fun `given currentSession is called, when localDataSource returns an entity, then maps the entity to Session and returns it`() {
        every { sessionMapper.fromSessionEntity(sessionEntity) } returns session
        coEvery { localDataSource.currentSession() } returns Either.Right(sessionEntity)

        val result = runBlocking { sessionDataSource.currentSession() }

        result shouldSucceed { it shouldBe session }
        verify(exactly = 1) { sessionMapper.fromSessionEntity(sessionEntity) }
    }

    @Test
    fun `given currentSession is called, when localDataSource returns a failure, then directly propagates the failure`() {
        val failure = mockk<Failure>()
        coEvery { localDataSource.currentSession() } returns Either.Left(failure)

        val result = runBlocking { sessionDataSource.currentSession() }

        result shouldFail { it shouldBe failure }
        verify { sessionMapper wasNot Called }
    }

    @Test
    fun `given accessToken is called, when remoteDataSource is successful, then maps the response and returns session`() {
        val accessTokenResponse = mockk<AccessTokenResponse>()
        val refreshToken = "refresh-token-123"
        coEvery { remoteDataSource.accessToken(refreshToken) } returns Either.Right(accessTokenResponse)
        every { sessionMapper.fromAccessTokenResponse(accessTokenResponse, refreshToken) } returns session

        val result = runBlocking { sessionDataSource.accessToken(refreshToken) }

        result shouldSucceed { it shouldBe session }
        coVerify(exactly = 1) { remoteDataSource.accessToken(refreshToken) }
        verify(exactly = 1) { sessionMapper.fromAccessTokenResponse(accessTokenResponse, refreshToken) }
    }

    @Test
    fun `given accessToken is called, when remoteDataSource fails, then directly propagates that failure`() {
        val refreshToken = "refresh-token-123"
        coEvery { remoteDataSource.accessToken(refreshToken) } returns Either.Left(ServerError)

        val result = runBlocking { sessionDataSource.accessToken(refreshToken) }

        result shouldFail { it shouldBe ServerError }
        coVerify(exactly = 1) { remoteDataSource.accessToken(refreshToken) }
        verify { sessionMapper wasNot Called }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when localDataSource successfully returns true, then propagates the result`() =
        testDoesCurrentSessionExistSuccessCase(true)

    @Test
    fun `given doesCurrentSessionExist is called, when localDataSource successfully returns false, then propagates the result`() =
        testDoesCurrentSessionExistSuccessCase(false)

    private fun testDoesCurrentSessionExistSuccessCase(exists: Boolean) {
        coEvery { localDataSource.doesCurrentSessionExist() } returns Either.Right(exists)

        val result = runBlocking { sessionDataSource.doesCurrentSessionExist() }

        result shouldSucceed { it shouldBe exists }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when localDataSource returns a failure, then propagates the failure`() {
        val failure = mockk<Failure>()
        coEvery { localDataSource.doesCurrentSessionExist() } returns Either.Left(failure)

        val result = runBlocking { sessionDataSource.doesCurrentSessionExist() }

        result shouldFail { it shouldBe failure }
    }
}
