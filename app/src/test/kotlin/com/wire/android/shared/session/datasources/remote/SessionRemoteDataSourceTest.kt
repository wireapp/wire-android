package com.wire.android.shared.session.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@Suppress("UNCHECKED_CAST")
class SessionRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var sessionApi: SessionApi

    @MockK
    private lateinit var accessTokenResponse: Response<AccessTokenResponse>

    @MockK
    private lateinit var accessTokenResponseBody: AccessTokenResponse

    private lateinit var sessionRemoteDataSource: SessionRemoteDataSource

    @Before
    fun setUp() {
        sessionRemoteDataSource = SessionRemoteDataSource(connectedNetworkHandler, sessionApi)
    }

    @Test
    fun `given accessToken is called, when sessionApi returns response, then returns the response body as success`() {
        coEvery { accessTokenResponse.isSuccessful } returns true
        coEvery { accessTokenResponse.body() } returns accessTokenResponseBody
        coEvery { sessionApi.accessToken(any()) } returns accessTokenResponse

        runBlocking {
            val result = sessionRemoteDataSource.accessToken(TEST_REFRESH_TOKEN)

            result shouldSucceed { it shouldBe accessTokenResponseBody }
            coVerify { sessionApi.accessToken("$REFRESH_TOKEN_HEADER_PREFIX$TEST_REFRESH_TOKEN") }
        }
    }

    @Test
    fun `given accessToken is called, when sessionApi returns failure, then propagates the failure`() {
        coEvery { accessTokenResponse.isSuccessful } returns false
        coEvery { accessTokenResponse.code() } returns 999
        coEvery { sessionApi.accessToken(any()) } returns accessTokenResponse

        runBlocking {
            val result = sessionRemoteDataSource.accessToken(TEST_REFRESH_TOKEN)

            result.isLeft shouldBe true
            coVerify { sessionApi.accessToken("$REFRESH_TOKEN_HEADER_PREFIX$TEST_REFRESH_TOKEN") }
        }
    }

    companion object {
        private const val TEST_REFRESH_TOKEN = "123-refresh-token-890"
        private const val REFRESH_TOKEN_HEADER_PREFIX = "zuid="
    }
}
