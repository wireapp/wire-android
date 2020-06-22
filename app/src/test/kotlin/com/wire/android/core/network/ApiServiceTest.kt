package com.wire.android.core.network

import com.wire.android.UnitTest
import com.wire.android.core.exception.*
import com.wire.android.core.extension.EMPTY
import com.wire.android.framework.functional.assertLeft
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
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
        apiService = object : ApiService() {
            override val networkHandler: NetworkHandler = this@ApiServiceTest.networkHandler
        }
    }

    @Test
    fun `given no network connection, when request called, returns NetworkConnection failure immediately`() {
        runBlocking {
            `when`(networkHandler.isConnected).thenReturn(false)

            val result = apiService.request(default = null, call = responseFunc)

            verifyNoInteractions(responseFunc)
            result.assertLeft { assertThat(it).isEqualTo(NetworkConnection) }
        }
    }

    @Test
    fun `given no default argument, when response is successful but has no body, returns EmptyResponseBody failure`() {
        runBlocking {
            `when`(networkHandler.isConnected).thenReturn(true)

            `when`(response.isSuccessful).thenReturn(true)
            `when`(response.body()).thenReturn(null)
            val responseFunc: suspend () -> Response<String> = { response }

            val result = apiService.request(default = null, call = responseFunc)

            result.assertLeft { assertThat(it).isEqualTo(EmptyResponseBody) }
        }
    }

    @Test
    fun `given call fails with http 400 error, then returns BadRequest failure`() = runBlocking {
        assertHttpError(400, BadRequest)
    }

    @Test
    fun `given call fails with http 401 error, then returns Unauthorized failure`() = runBlocking {
        assertHttpError(401, Unauthorized)
    }

    @Test
    fun `given call fails with http 403 error, then returns Forbidden failure`() = runBlocking {
        assertHttpError(403, Forbidden)
    }

    @Test
    fun `given call fails with http 404 error, then returns NotFound failure`() = runBlocking {
        assertHttpError(404, NotFound)
    }

    @Test
    fun `given call fails with http 500 error, then returns InternalServerError failure`() = runBlocking {
        assertHttpError(500, InternalServerError)
    }

    @Test
    fun `given call fails with any other error, then returns ServerError failure`() = runBlocking {
        assertHttpError(-1, ServerError)
    }

    private suspend fun assertHttpError(httpErrorCode: Int, failure: Failure) {
        `when`(networkHandler.isConnected).thenReturn(true)

        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.code()).thenReturn(httpErrorCode)
        val responseFunc: suspend () -> Response<String> = { response }

        val result = apiService.request(String.EMPTY, responseFunc)

        result.assertLeft { assertThat(it).isEqualTo(failure) }
    }
}
