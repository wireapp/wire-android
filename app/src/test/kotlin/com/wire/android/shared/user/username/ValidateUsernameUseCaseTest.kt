package com.wire.android.shared.user.username

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.map
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class ValidateUsernameUseCaseTest : UnitTest() {

    private lateinit var validateHandleUseCase: ValidateUsernameUseCase

    @MockK
    private lateinit var validateHandleParams: ValidateUsernameParams

    @Before
    fun setup() {
        validateHandleUseCase = ValidateUsernameUseCase()
    }

    @Test
    fun `Given run is executed, when username doesn't match regex, then propagate failure`() {
        val username = "----7_.username"
        verifyValidateUseCase(username)
    }

    @Test
    fun `Given run is executed, when username matches regex and length is over max, then propagate failure`() {
        val username = """"thisisalongusernamethatshouldnotbethislongthisisalongusernamethatshouldnotbethislongthisisalongusernamethatsho
                         | islongthisisalongusernamethatshouldnotbethislongthisisalongusernamethatshouldnotbethislongthisisalongusernamethat
                     """".trimMargin()
        verifyValidateUseCase(username)
    }

    @Test
    fun `Given run is executed, when username matches regex and length is 1, then propagate failure`() {
        val username = "h"
        verifyValidateUseCase(username)
    }

    @Test
    fun `Given run is executed, when username is empty then propagate failure`() {
        val username = String.EMPTY
        verifyValidateUseCase(username)
    }

    @Test
    fun `Given run is executed, when username matches regex and username fits requirements then propagate success`() {
        val username = "wire"
        verifyValidateUseCase(username, isError = false)
    }

    private fun verifyValidateUseCase(username: String, isError: Boolean = true) = runBlocking {
        every { validateHandleParams.username } returns username

        validateHandleUseCase.run(validateHandleParams)

        if (!isError) {
            validateHandleUseCase.run(validateHandleParams).map {
                it shouldBe username
            }
        }

        validateHandleUseCase.run(validateHandleParams).isLeft shouldBe isError
    }
}
