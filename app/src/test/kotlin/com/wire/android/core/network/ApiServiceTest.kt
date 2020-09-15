package com.wire.android.core.network

import com.wire.android.UnitTest
import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.EmptyResponseBody
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.InternalServerError
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.NotFound
import com.wire.android.core.exception.ServerError
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.exception.Unauthorized
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import retrofit2.Response

class ApiServiceTest : UnitTest() {

    private lateinit var apiService: ApiService

    @Mock
    private lateinit var networkHandler: NetworkHandler

    @Mock
    private lateinit var response: Response<String>

    @Mock
    private lateinit var responseFunc: suspend () -> Response<String>

    @Before
    fun setUp() {
        `when`(networkHandler.isConnected).thenReturn(true)
        apiService = object : ApiService() {
            override val networkHandler: NetworkHandler = this@ApiServiceTest.networkHandler
        }
    }

    @Test
    fun `given rawRequest is called, when there's no network connection, then returns NetworkConnection failure immediately`() {
        runBlocking {
            `when`(networkHandler.isConnected).thenReturn(false)

            val result = apiService.rawRequest(responseFunc)

            verifyNoInteractions(responseFunc)
            result.assertLeft { assertThat(it).isEqualTo(NetworkConnection) }
        }
    }

    @Test
    fun `given rawRequest is called, when response is successful, then returns the response`() {
        runBlocking {
            `when`(response.isSuccessful).thenReturn(true)

            val result = apiService.rawRequest(::testCall)

            result.assertRight { assertThat(it).isEqualTo(response) }
        }
    }

    @Test
    fun `given request is called with a default value, when response is successful and has a body, then returns the body`() {
        runBlocking {
            `when`(response.isSuccessful).thenReturn(true)
            `when`(response.body()).thenReturn(TEST_BODY)

            val result = apiService.request(default = TEST_DEFAULT_ARGUMENT, call = ::testCall)

            result.assertRight { assertThat(it).isEqualTo(TEST_BODY) }
        }
    }

    @Test
    fun `given request is called with a default value, when response is successful but has no body, then returns default value`() {
        runBlocking {
            `when`(response.isSuccessful).thenReturn(true)
            `when`(response.body()).thenReturn(null)

            val result = apiService.request(default = TEST_DEFAULT_ARGUMENT, call = ::testCall)

            result.assertRight { assertThat(it).isEqualTo(TEST_DEFAULT_ARGUMENT) }
        }
    }

    @Test
    fun `given request is called without default value, when response successful but has no body, then returns EmptyResponseBody error`() {
        runBlocking {
            `when`(response.isSuccessful).thenReturn(true)
            `when`(response.body()).thenReturn(null)

            val result = apiService.request(default = null, call = ::testCall)

            result.assertLeft { assertThat(it).isEqualTo(EmptyResponseBody) }
        }
    }

    @Test
    fun `given rawRequest is called, when call fails with http 400 error, then returns BadRequest failure`() =
        assertHttpError(400, BadRequest)

    @Test
    fun `given rawRequest is called, when call fails with http 401 error, then returns Unauthorized failure`() =
        assertHttpError(401, Unauthorized)

    @Test
    fun `given rawRequest is called, when call fails with http 403 error, then returns Forbidden failure`() =
        assertHttpError(403, Forbidden)

    @Test
    fun `given rawRequest is called, when call fails with http 404 error, then returns NotFound failure`() =
        assertHttpError(404, NotFound)

    @Test
    fun `given rawRequest is called, when call fails with http 429 error, then returns TooManyRequests failure`() =
        assertHttpError(429, TooManyRequests)

    @Test
    fun `given rawRequest is called, when call fails with http 500 error, then returns InternalServerError failure`() =
        assertHttpError(500, InternalServerError)

    @Test
    fun `given rawRequest is called, when call fails with any other error, then returns ServerError failure`() =
        assertHttpError(-1, ServerError)

    private fun assertHttpError(httpErrorCode: Int, failure: Failure): Unit = runBlocking {
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.code()).thenReturn(httpErrorCode)

        val result = apiService.rawRequest(::testCall)

        result.assertLeft { assertThat(it).isEqualTo(failure) }
    }

    private suspend fun testCall(): Response<String> = response

    companion object {
        private const val TEST_DEFAULT_ARGUMENT = "default"
        private const val TEST_BODY = """ { "email": "test@wire.com" } """
    }
}
