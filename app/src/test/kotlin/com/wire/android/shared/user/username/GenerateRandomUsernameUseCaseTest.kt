package com.wire.android.shared.user.username

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class GenerateRandomUsernameUseCaseTest : UnitTest() {

    private lateinit var generateUsernameUseCase: GenerateRandomUsernameUseCase

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var usernameAttemptsGenerator: UsernameAttemptsGenerator

    @MockK
    private lateinit var session: Session

    @MockK
    private lateinit var user: User

    @Before
    fun setup() {
        generateUsernameUseCase = GenerateRandomUsernameUseCase(sessionRepository, userRepository, usernameAttemptsGenerator)
    }

    @Test
    fun `given run, when generated username exists, propagate new username`() {
        val listOfUsernames = listOf<String>(TEST_USERNAME)

        coEvery { sessionRepository.currentSession() } returns Either.Right(session)
        coEvery { userRepository.userById(any()) } returns Either.Right(user)
        coEvery { userRepository.checkUsernamesExist(any()) } returns Either.Right(listOfUsernames)

        val result = runBlocking { generateUsernameUseCase.run(Unit) }

        result.shouldSucceed { it shouldBeEqualTo TEST_USERNAME }
    }

    @Test
    fun `given run, when current session fails, then propagate failure`() {
        coEvery { sessionRepository.currentSession() } returns Either.Left(ServerError)

        val result = runBlocking { generateUsernameUseCase.run(Unit) }

        result.shouldFail { it shouldBe ServerError }
    }

    @Test
    fun `given run, when userById fails, then propagate failure`() {
        coEvery { sessionRepository.currentSession() } returns Either.Right(session)
        coEvery { userRepository.userById(any()) } returns Either.Left(ServerError)

        val result = runBlocking { generateUsernameUseCase.run(Unit) }

        result.shouldFail { it shouldBe ServerError }
    }

    @Test
    fun `given run, when checkUsername fails, then propagate failure`() {
        coEvery { sessionRepository.currentSession() } returns Either.Right(session)
        coEvery { userRepository.userById(any()) } returns Either.Right(user)
        coEvery { userRepository.checkUsernamesExist(any()) } returns Either.Left(ServerError)

        val result = runBlocking { generateUsernameUseCase.run(Unit) }

        result.shouldFail { it shouldBe ServerError }
    }

    @Test
    fun `given run, when checkUsername returns empty list, then propagate NoAvailableUsernames failure`() {
        coEvery { sessionRepository.currentSession() } returns Either.Right(session)
        coEvery { userRepository.userById(any()) } returns Either.Right(user)
        coEvery { userRepository.checkUsernamesExist(any()) } returns Either.Right(emptyList())

        val result = runBlocking { generateUsernameUseCase.run(Unit) }

        result.shouldFail { it shouldBe NoAvailableUsernames }
    }

    companion object {
        private const val TEST_USERNAME = "MonkeyMaddness274578"
    }
}
