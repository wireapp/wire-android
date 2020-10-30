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
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ApiServiceTest : UnitTest() {

    private lateinit var apiService: ApiService

    @MockK
    private lateinit var networkHandler: NetworkHandler

    @MockK
    private lateinit var response: Response<String>

    @Before
    fun setUp() {
        every { networkHandler.isConnected } returns true
        apiService = object : ApiService() {
            override val networkHandler: NetworkHandler = this@ApiServiceTest.networkHandler
        }
    }

    @Test
    fun `given rawRequest is called, when there's no network connection, then returns NetworkConnection failure immediately`() {
        val responseFunc: suspend () -> Response<String> = mockk(relaxed = true)
        every { networkHandler.isConnected } returns false

        val result = runBlocking { apiService.rawRequest(responseFunc) }

        verify { responseFunc wasNot Called }
        result shouldFail { it shouldBe NetworkConnection }
    }

    @Test
    fun `given rawRequest is called, when response is successful, then returns the response`() {
        every { response.isSuccessful } returns true

        val result = runBlocking { apiService.rawRequest(::testCall) }

        result shouldSucceed { it shouldBe response }
    }

    @Test
    fun `given request is called with a default value, when response is successful and has a body, then returns the body`() {
        every { response.isSuccessful } returns true
        every { response.body() } returns TEST_BODY

        val result = runBlocking { apiService.request(default = TEST_DEFAULT_ARGUMENT, call = ::testCall) }

        result shouldSucceed { it shouldBe TEST_BODY }
    }

    @Test
    fun `given request is called with a default value, when response is successful but has no body, then returns default value`() {
        every { response.isSuccessful } returns true
        every { response.body() } returns null

        val result = runBlocking { apiService.request(default = TEST_DEFAULT_ARGUMENT, call = ::testCall) }

        result shouldSucceed { it shouldBe TEST_DEFAULT_ARGUMENT }
    }

    @Test
    fun `given request is called without default value, when response successful but has no body, then returns EmptyResponseBody error`() {
        every { response.isSuccessful } returns true
        every { response.body() } returns null

        val result = runBlocking { apiService.request(default = null, call = ::testCall) }

        result shouldFail { it shouldBe EmptyResponseBody }
    }

    @Test
    fun `given rawRequest is called, when call fails with http 400 error, then returns BadRequest failure`() =
        shouldTriggerHttpError(400, BadRequest)

    @Test
    fun `given rawRequest is called, when call fails with http 401 error, then returns Unauthorized failure`() =
        shouldTriggerHttpError(401, Unauthorized)

    @Test
    fun `given rawRequest is called, when call fails with http 403 error, then returns Forbidden failure`() =
        shouldTriggerHttpError(403, Forbidden)

    @Test
    fun `given rawRequest is called, when call fails with http 404 error, then returns NotFound failure`() =
        shouldTriggerHttpError(404, NotFound)

    @Test
    fun `given rawRequest is called, when call fails with http 429 error, then returns TooManyRequests failure`() =
        shouldTriggerHttpError(429, TooManyRequests)

    @Test
    fun `given rawRequest is called, when call fails with http 500 error, then returns InternalServerError failure`() =
        shouldTriggerHttpError(500, InternalServerError)

    @Test
    fun `given rawRequest is called, when call fails with any other error, then returns ServerError failure`() =
        shouldTriggerHttpError(-1, ServerError)

    private fun shouldTriggerHttpError(httpErrorCode: Int, failure: Failure) {
        every { response.isSuccessful } returns false
        every { response.code() } returns httpErrorCode

        val result = runBlocking { apiService.rawRequest(::testCall) }

        result shouldFail { it shouldBe failure }
    }

    private suspend fun testCall(): Response<String> = response

    companion object {
        private const val TEST_DEFAULT_ARGUMENT = "default"
        private const val TEST_BODY = """ { "email": "test@wire.com" } """
    }
}
