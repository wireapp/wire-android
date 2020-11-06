package com.wire.android.shared.user.name

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test


class ValidateNameUseCaseTest : UnitTest() {

    private lateinit var validateNameUseCase: ValidateNameUseCase

    @Before
    fun setUp() {
        validateNameUseCase = ValidateNameUseCase()
    }

    @Test
    fun `given params with a name shorter than 2 characters, when run() is called, then return NameTooShort error`() {
        val name = "a"

        val result = runBlocking { validateNameUseCase.run(ValidateNameParams(name)) }

        result shouldFail { it shouldBeEqualTo NameTooShort }
    }

    @Test
    fun `given params with a name longer than 1 character, when run() is called, then return success`() {
        val name = "ab"

        val result = runBlocking { validateNameUseCase.run(ValidateNameParams(name)) }

        result shouldSucceed { it shouldBeEqualTo Unit }
    }
}
