package com.wire.android.shared.user.username

import com.wire.android.UnitTest
import com.wire.android.shared.user.UserRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
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
    fun `Given run is executed, then request repository doesUsernameExist`() = runBlocking {
        every { params.username } returns TEST_USERNAME

        checkUsernameExistsUseCase.run(params)

        coVerify { userRepository.doesUsernameExist(eq(TEST_USERNAME)) }
    }

    companion object {
        private const val TEST_USERNAME = "username"
    }
}
