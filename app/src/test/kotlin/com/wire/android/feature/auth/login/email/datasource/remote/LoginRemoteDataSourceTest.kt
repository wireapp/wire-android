package com.wire.android.feature.auth.login.email.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.core.functional.onSuccess
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.shared.auth.remote.LabelGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class LoginRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var loginApi: LoginApi

    @MockK
    private lateinit var labelGenerator: LabelGenerator

    private lateinit var loginRemoteDataSource: LoginRemoteDataSource

    @Before
    fun setUp() {
        every { labelGenerator.newLabel() } returns TEST_LABEL
        loginRemoteDataSource = LoginRemoteDataSource(loginApi, labelGenerator, connectedNetworkHandler)
    }

    @Test
    fun `given credentials, when loginWithEmail is called, then calls loginApi with correct credentials and a new label`() {
        val loginWithEmailRequestSlot = slot<LoginWithEmailRequest>()
        val response = mockSuccessResponse()
        coEvery { loginApi.loginWithEmail(LoginWithEmailRequest(TEST_EMAIL, TEST_PASSWORD, TEST_LABEL)) } returns response

        runBlocking { loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) }

        verify(exactly = 1) { labelGenerator.newLabel() }
        coVerify(exactly = 1) { loginApi.loginWithEmail(capture(loginWithEmailRequestSlot), eq(true)) }
        loginWithEmailRequestSlot.captured.let {
            it.email shouldBe TEST_EMAIL
            it.password shouldBe TEST_PASSWORD
            it.label shouldBe TEST_LABEL
        }
    }

    @Test
    fun `given api returns success, when loginWithEmail is called, then returns the response`() {
        val response = mockSuccessResponse()
        coEvery { loginApi.loginWithEmail(LoginWithEmailRequest(TEST_EMAIL, TEST_PASSWORD, TEST_LABEL)) } returns response

        val result = runBlocking { loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) }

        result shouldSucceed { it shouldBe response }
    }

    @Test
    fun `given api returns failure, when loginWithEmail is called, then returns failure`() {
        val response = mockErrorResponse(errorCode = 404)
        coEvery { loginApi.loginWithEmail(LoginWithEmailRequest(TEST_EMAIL, TEST_PASSWORD, TEST_LABEL)) } returns response

        val result = runBlocking { loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD) }

        result.onSuccess { fail("Expected failure but got success") }
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "ssdf34"
        private const val TEST_LABEL = "sdlkf032"

        private fun mockSuccessResponse(): Response<LoginWithEmailResponse> =
            (mockkClass(Response::class) as Response<LoginWithEmailResponse>).apply {
                every { isSuccessful } returns true
            }

        private fun mockErrorResponse(errorCode: Int): Response<LoginWithEmailResponse> =
            (mockkClass(Response::class) as Response<LoginWithEmailResponse>).apply {
                every { isSuccessful } returns false
                every { code() } returns errorCode
            }
    }
}
