package com.wire.android.ui.newauthentication.login

import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeResult
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidateEmailOrSSOCodeUseCaseTest {

    @Test
    fun `given an input, when is an email and valid, then return true`() {
        val (arrangement, sut) = Arrangement()
            .withValidateEmailUseCaseReturning(true)
            .arrange()

        val result = sut.invoke("user@wire.com")

        verify { arrangement.validateEmailUseCase(any()) }
        verify(exactly = 0) { arrangement.validateSSOCodeUseCase(any()) }
        assertEquals(true, result)
    }

    @Test
    fun `given an input, when is an email and invalid, then return false`() {
        val (arrangement, sut) = Arrangement()
            .withValidateEmailUseCaseReturning(false)
            .arrange()

        val result = sut.invoke("user@")

        verify { arrangement.validateEmailUseCase(any()) }
        verify(exactly = 0) { arrangement.validateSSOCodeUseCase(any()) }
        assertEquals(false, result)
    }

    @Test
    fun `given an input, when is a SSO code and valid, then return true`() {
        val code = "70488875-13dd-4ba7-9636-a983e1831f5f"
        val (arrangement, sut) = Arrangement()
            .withValidateSSOCodeUseCaseReturning(ValidateSSOCodeResult.Valid(code))
            .arrange()

        val result = sut.invoke("wire-$code")

        verify { arrangement.validateSSOCodeUseCase(any()) }
        verify(exactly = 0) { arrangement.validateEmailUseCase(any()) }
        assertEquals(true, result)
    }

    @Test
    fun `given an input, when is a SSO code and invalid, then return false`() {
        val code = "7not-valid-code"
        val (arrangement, sut) = Arrangement()
            .withValidateSSOCodeUseCaseReturning(ValidateSSOCodeResult.Invalid)
            .arrange()

        val result = sut.invoke("wire-$code")

        verify { arrangement.validateSSOCodeUseCase(any()) }
        verify(exactly = 0) { arrangement.validateEmailUseCase(any()) }
        assertEquals(false, result)
    }

    @Test
    fun `given an input, when is invalid email or sso code, then return false`() {
        val (arrangement, sut) = Arrangement()
            .withValidateSSOCodeUseCaseReturning(ValidateSSOCodeResult.Invalid)
            .withValidateEmailUseCaseReturning(false)
            .arrange()

        val result = sut.invoke("nothing-valid")

        verify(exactly = 1) { arrangement.validateEmailUseCase(any()) }
        verify(exactly = 0) { arrangement.validateSSOCodeUseCase(any()) }
        assertEquals(false, result)
    }

    private class Arrangement {

        val validateEmailUseCase: ValidateEmailUseCase = mockk()
        val validateSSOCodeUseCase: ValidateSSOCodeUseCase = mockk()

        fun withValidateEmailUseCaseReturning(result: Boolean = true) = apply {
            every { validateEmailUseCase(any()) } returns result
        }

        fun withValidateSSOCodeUseCaseReturning(result: ValidateSSOCodeResult = ValidateSSOCodeResult.Invalid) = apply {
            every { validateSSOCodeUseCase(any()) } returns result
        }

        fun arrange() = this to ValidateEmailOrSSOCodeUseCase(validateEmailUseCase, validateSSOCodeUseCase)
    }

}
