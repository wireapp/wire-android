package com.wire.android.shared.user.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class GetCurrentUserUseCaseTest : UnitTest() {

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var userRepository: UserRepository

    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @Before
    fun setUp() {
        getCurrentUserUseCase = GetCurrentUserUseCase(sessionRepository, userRepository)
    }

    @Test
    fun `given run is called, when sessionRepository fails to fetch current session, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.currentSession() } returns Either.Left(failure)

        val result = runBlocking { getCurrentUserUseCase.run(Unit) }

        result shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.currentSession() }
        verify { userRepository wasNot Called }
    }

    @Test
    fun `given run is called, when userRepository fails to get user, then propagates failure`() {
        val session = mockk<Session>()
        every { session.userId } returns TEST_USER_ID
        coEvery { sessionRepository.currentSession() } returns Either.Right(session)

        val failure = mockk<Failure>()
        coEvery { userRepository.userById(TEST_USER_ID) } returns Either.Left(failure)

        val result = runBlocking { getCurrentUserUseCase.run(Unit) }

        result shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.currentSession() }
        coVerify(exactly = 1) { userRepository.userById(TEST_USER_ID) }
    }

    @Test
    fun `given run is called, when userRepository returns user, then propagates user`() {
        val session = mockk<Session>()
        every { session.userId } returns TEST_USER_ID
        coEvery { sessionRepository.currentSession() } returns Either.Right(session)

        val user = mockk<User>()
        coEvery { userRepository.userById(TEST_USER_ID) } returns Either.Right(user)

        val result = runBlocking { getCurrentUserUseCase.run(Unit) }

        result shouldSucceed  { it shouldBeEqualTo user}
        coVerify(exactly = 1) { sessionRepository.currentSession() }
        coVerify(exactly = 1) { userRepository.userById(TEST_USER_ID) }
    }

    companion object {
        private const val TEST_USER_ID = "user-id-3459"
    }
}
