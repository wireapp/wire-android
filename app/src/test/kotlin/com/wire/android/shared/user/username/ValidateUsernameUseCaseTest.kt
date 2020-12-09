package com.wire.android.shared.user.username

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
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
    fun `Given run is executed, when username doesn't match regex, then propagate failure`() = runBlocking {
        val username = "----7_.username"
        every { validateHandleParams.username } returns username

        val response = validateHandleUseCase.run(validateHandleParams)

        response shouldFail { it shouldBe UsernameInvalid }
    }

    @Test
    fun `Given run is executed, when username matches regex and length is over max, then propagate failure`() = runBlocking {
        val username = "thisisalongusernamethatshouldnotbethislongthisisalongusernamethatshouldnotbethislongthisisalongusernamethat" +
                "thisisalongusernamethatshouldnotbethislongthisisalongusernamethatshouldnotbethislongthisisalongusernamethat" +
                "thisisalongusernamethatshouldnotbethislongthisisalongusernamethatshouldnotbethislongthisisalongusernamethat"

        every { validateHandleParams.username } returns username

        val response = validateHandleUseCase.run(validateHandleParams)

        response shouldFail { it shouldBe UsernameTooLong }
    }

    @Test
    fun `Given run is executed, when username matches regex and length is 1, then propagate failure`() = runBlocking {
        val username = "h"
        every { validateHandleParams.username } returns username

        val response = validateHandleUseCase.run(validateHandleParams)

        response shouldFail { it shouldBe UsernameTooShort }
    }

    @Test
    fun `Given run is executed, when username is empty then propagate failure`() = runBlocking {
        val username = String.EMPTY
        every { validateHandleParams.username } returns username

        val response = validateHandleUseCase.run(validateHandleParams)

        response shouldFail { it shouldBe UsernameTooShort }
    }

    @Test
    fun `Given run is executed, when username matches regex and username fits requirements then propagate success`() = runBlocking {
        val username = "wire"
        every { validateHandleParams.username } returns username

        val response = validateHandleUseCase.run(validateHandleParams)

        response.shouldSucceed { it shouldBe username }
    }
}
