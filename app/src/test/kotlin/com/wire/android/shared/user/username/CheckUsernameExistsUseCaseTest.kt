package com.wire.android.shared.user.username

import com.wire.android.UnitTest
import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.NotFound
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.user.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class CheckUsernameExistsUseCaseTest : UnitTest() {

    private lateinit var checkUsernameExistsUseCase: CheckUsernameExistsUseCase

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var params: CheckUsernameExistsParams

    @Before
    fun setup() {
        checkUsernameExistsUseCase = CheckUsernameExistsUseCase(userRepository)
    }

    @Test
    fun `Given doesUsernameExist() is called, when response code is NotFound, then propagate UsernameIsAvailable success`() = runBlocking {
        every { params.username } returns TEST_USERNAME
        coEvery { userRepository.doesUsernameExist(any()) } returns Either.Left(NotFound)

        val response = checkUsernameExistsUseCase.run(params)

        coVerify { userRepository.doesUsernameExist(eq(TEST_USERNAME)) }

        response shouldSucceed { it shouldBe TEST_USERNAME }
    }

    @Test
    fun `Given run is called, when response is success, then propagate a UsernameAlreadyExists failure`() = runBlocking {
        every { params.username } returns TEST_USERNAME
        coEvery { userRepository.doesUsernameExist(any()) } returns Either.Right(Unit)

        val response = checkUsernameExistsUseCase.run(params)
        coVerify { userRepository.doesUsernameExist(eq(TEST_USERNAME)) }

        response shouldFail { it shouldBe UsernameAlreadyExists }
    }

    @Test
    fun `Given run is called, when response code is BadRequest, then propagate a UsernameInvalid failure`() = runBlocking {
        every { params.username } returns TEST_USERNAME
        coEvery { userRepository.doesUsernameExist(any()) } returns Either.Left(BadRequest)

        val response = checkUsernameExistsUseCase.run(params)

        coVerify { userRepository.doesUsernameExist(eq(TEST_USERNAME)) }

        response shouldFail { it shouldBe UsernameInvalid }

    }

    @Test
    fun `Given run is called, when response is a generic failure, then propagate that failure`() = runBlocking {
        every { params.username } returns TEST_USERNAME
        coEvery { userRepository.doesUsernameExist(any()) } returns Either.Left(ServerError)

        val response = checkUsernameExistsUseCase.run(params)

        coVerify { userRepository.doesUsernameExist(eq(TEST_USERNAME)) }

        response shouldFail { it shouldBe ServerError }
    }

    companion object {
        private const val TEST_USERNAME = "username"
    }
}
