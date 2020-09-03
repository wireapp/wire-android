package com.wire.android.feature.auth.login.email.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.capture
import com.wire.android.core.functional.onSuccess
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.shared.auth.remote.LabelGenerator
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import retrofit2.Response

class LoginRemoteDataSourceTest : UnitTest() {

    @Mock
    private lateinit var loginApi: LoginApi

    @Mock
    private lateinit var labelGenerator: LabelGenerator

    @Captor
    private lateinit var loginWithEmailRequestCaptor: ArgumentCaptor<LoginWithEmailRequest>

    private lateinit var loginRemoteDataSource: LoginRemoteDataSource

    @Before
    fun setUp() {
        `when`(labelGenerator.newLabel()).thenReturn(TEST_LABEL)
        loginRemoteDataSource = LoginRemoteDataSource(loginApi, labelGenerator, connectedNetworkHandler)
    }

    @Test
    fun `given credentials, when loginWithEmail is called, then calls loginApi with correct credentials and a new label`() {
        runBlocking {
            val response = mockSuccessResponse()
            `when`(loginApi.loginWithEmail(LoginWithEmailRequest(TEST_EMAIL, TEST_PASSWORD, TEST_LABEL))).thenReturn(response)

            loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            verify(labelGenerator).newLabel()
            verify(loginApi).loginWithEmail(capture(loginWithEmailRequestCaptor))
            loginWithEmailRequestCaptor.value.let {
                assertThat(it.email).isEqualTo(TEST_EMAIL)
                assertThat(it.password).isEqualTo(TEST_PASSWORD)
                assertThat(it.label).isEqualTo(TEST_LABEL)
            }
        }
    }

    @Test
    fun `given api returns success, when loginWithEmail is called, then returns the response`() {
        runBlocking {
            val response = mockSuccessResponse()
            `when`(loginApi.loginWithEmail(LoginWithEmailRequest(TEST_EMAIL, TEST_PASSWORD, TEST_LABEL))).thenReturn(response)

            val result = loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            result.assertRight {
                assertThat(it).isInstanceOf(LoginWithEmailResponse::class.java)
            }
        }
    }

    @Test
    fun `given api returns failure, when loginWithEmail is called, then returns failure`() {
        runBlocking {
            val response = mockErrorResponse(errorCode = 404)
            `when`(loginApi.loginWithEmail(LoginWithEmailRequest(TEST_EMAIL, TEST_PASSWORD, TEST_LABEL))).thenReturn(response)

            val result = loginRemoteDataSource.loginWithEmail(TEST_EMAIL, TEST_PASSWORD)

            result.onSuccess { fail("Expected failure but got success") }
        }
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "ssdf34"
        private const val TEST_LABEL = "sdlkf032"
        private val LOGIN_WITH_EMAIL_RESPONSE = LoginWithEmailResponse(
            expiresIn = 900,
            accessToken = "AccessToken",
            userId = "123",
            tokenType = "Bearer"
        )

        private fun mockSuccessResponse(): Response<LoginWithEmailResponse> =
            (mock(Response::class.java) as Response<LoginWithEmailResponse>).apply {
                `when`(this.isSuccessful).thenReturn(true)
                `when`(this.body()).thenReturn(LOGIN_WITH_EMAIL_RESPONSE)
            }

        private fun mockErrorResponse(errorCode: Int): Response<LoginWithEmailResponse> =
            (mock(Response::class.java) as Response<LoginWithEmailResponse>).apply {
                `when`(this.isSuccessful).thenReturn(false)
                `when`(this.code()).thenReturn(errorCode)
            }
    }
}
