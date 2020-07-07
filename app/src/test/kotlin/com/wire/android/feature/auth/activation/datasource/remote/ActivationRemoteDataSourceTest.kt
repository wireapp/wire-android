package com.wire.android.feature.auth.activation.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.capture
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.network.connectedNetworkHandler
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import retrofit2.Response

class ActivationRemoteDataSourceTest : UnitTest() {

    private lateinit var activationRemoteDataSource: ActivationRemoteDataSource

    @Mock
    private lateinit var activationApi: ActivationApi

    @Mock
    private lateinit var response: Response<Unit>

    @Captor
    private lateinit var sendEmailActivationCodeRequestCaptor: ArgumentCaptor<SendEmailActivationCodeRequest>

    @Captor
    private lateinit var emailActivationRequestCaptor: ArgumentCaptor<EmailActivationRequest>

    @Before
    fun setUp() {
        activationRemoteDataSource = ActivationRemoteDataSource(activationApi, connectedNetworkHandler)
    }

    @Test
    fun `Given an email, when sendEmailActivationCode() is called, calls activationApi with correct SendEmailActivationCodeRequest`() {
        runBlocking {
            activationRemoteDataSource.sendEmailActivationCode(TEST_EMAIL)

            verify(activationApi).sendActivationCode(capture(sendEmailActivationCodeRequestCaptor))

            assertThat(sendEmailActivationCodeRequestCaptor.value.email).isEqualTo(TEST_EMAIL)
        }
    }

    @Test
    fun `Given sendEmailActivationCode() is called, when api response is successful, then return success`() =
        runBlocking {
            `when`(response.body()).thenReturn(Unit)
            `when`(response.isSuccessful).thenReturn(true)
            `when`(activationApi.sendActivationCode(any())).thenReturn(response)

            val result = activationRemoteDataSource.sendEmailActivationCode(TEST_EMAIL)

            verify(activationApi).sendActivationCode(any())
            result.assertRight()
        }

    @Test
    fun `Given sendEmailActivationCode() is called, when api response fails, then return a failure`() {
        runBlocking {
            `when`(response.isSuccessful).thenReturn(false)
            `when`(activationApi.sendActivationCode(any())).thenReturn(response)

            val result = activationRemoteDataSource.sendEmailActivationCode(TEST_EMAIL)

            verify(activationApi).sendActivationCode(any())
            assertThat(result.isLeft).isTrue()
        }
    }

    @Test
    fun `Given an email and a code, when activateEmail() is called, calls activationApi with correct EmailActivationRequest`() {
        runBlocking {
            activationRemoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE)

            verify(activationApi).activateEmail(capture(emailActivationRequestCaptor))
            with(emailActivationRequestCaptor.value) {
                assertThat(email).isEqualTo(TEST_EMAIL)
                assertThat(code).isEqualTo(TEST_CODE)
                assertThat(dryrun).isEqualTo(true)
            }
        }
    }

    @Test
    fun `Given activateEmail() is called, when api response is successful, then return success`() =
        runBlocking {
            `when`(response.body()).thenReturn(Unit)
            `when`(response.isSuccessful).thenReturn(true)
            `when`(activationApi.activateEmail(any())).thenReturn(response)

            val result = activationRemoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE)

            verify(activationApi).activateEmail(any())
            result.assertRight()
        }

    @Test
    fun `Given activateEmail() is called, when api response fails, then return failure`() {
        runBlocking {
            `when`(response.isSuccessful).thenReturn(false)
            `when`(activationApi.activateEmail(any())).thenReturn(response)

            val result = activationRemoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE)

            verify(activationApi).activateEmail(any())
            assertThat(result.isLeft).isTrue()
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_CODE = "123456"
    }
}
