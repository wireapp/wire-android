package com.wire.android.shared.user.username

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class UpdateUsernameUseCaseTest : UnitTest() {

    private lateinit var updateUsernameUseCase: UpdateUsernameUseCase

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var updateUsernameParams: UpdateUsernameParams

    @MockK
    private lateinit var session: Session

    @Before
    fun setup() {
        updateUsernameUseCase = UpdateUsernameUseCase(sessionRepository, userRepository)
    }

    @Test
    fun `given run is called, when currentSession succeeds, then update username`() = runBlocking {
        every { updateUsernameParams.username } returns TEST_USERNAME
        every { session.userId } returns TEST_USER_ID
        coEvery { sessionRepository.currentSession() } returns Either.Right(session)
        coEvery { userRepository.updateUsername(any(), any()) } returns Either.Right(Unit)

        updateUsernameUseCase.run(updateUsernameParams)

        coVerify(exactly = 1) { sessionRepository.currentSession() }
        coVerify(exactly = 1) { userRepository.updateUsername(eq(TEST_USER_ID), eq(TEST_USERNAME)) }
    }

    @Test
    fun `given run is called, when currentSession fails, then do not update username`() = runBlocking {
        coEvery { sessionRepository.currentSession() } returns Either.Left(ServerError)

        updateUsernameUseCase.run(updateUsernameParams)

        coVerify(exactly = 1) { sessionRepository.currentSession() }
        coVerify(inverse = true) { userRepository.updateUsername(any(), any()) }
    }

    companion object {
        private const val TEST_USERNAME = "username"
        private const val TEST_USER_ID = "user-id"
    }
}