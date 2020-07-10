package com.wire.android.shared.user.name

import com.wire.android.UnitTest
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
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
        runBlocking {
            val name = "a"

            val result = validateNameUseCase.run(ValidateNameParams(name))

            result.assertLeft {
                assertThat(it).isEqualTo(NameTooShort)
            }
        }
    }

    @Test
    fun `given params with a name longer than 1 character, when run() is called, then return success`() {
        runBlocking {
            val name = "ab"

            val result = validateNameUseCase.run(ValidateNameParams(name))

            result.assertRight()
        }
    }
}
