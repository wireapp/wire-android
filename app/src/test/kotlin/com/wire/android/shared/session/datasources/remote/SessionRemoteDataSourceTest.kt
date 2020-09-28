package com.wire.android.shared.session.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.network.connectedNetworkHandler
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import retrofit2.Response

@Suppress("UNCHECKED_CAST")
class SessionRemoteDataSourceTest : UnitTest() {

    @Mock
    private lateinit var sessionApi: SessionApi

    @Mock
    private lateinit var accessTokenResponse: Response<AccessTokenResponse>

    @Mock
    private lateinit var accessTokenResponseBody: AccessTokenResponse

    private lateinit var sessionRemoteDataSource: SessionRemoteDataSource

    @Before
    fun setUp() {
        sessionRemoteDataSource = SessionRemoteDataSource(connectedNetworkHandler, sessionApi)
    }

    @Test
    fun `given accessToken is called, when sessionApi returns response, then returns the response body as success`() {
        runBlocking {
            `when`(accessTokenResponse.isSuccessful).thenReturn(true)
            `when`(accessTokenResponse.body()).thenReturn(accessTokenResponseBody)
            `when`(sessionApi.accessToken(any())).thenReturn(accessTokenResponse)

            val result = sessionRemoteDataSource.accessToken(TEST_REFRESH_TOKEN)

            result.assertRight {
                assertThat(it).isEqualTo(accessTokenResponseBody)
            }
            verify(sessionApi).accessToken("$REFRESH_TOKEN_HEADER_PREFIX$TEST_REFRESH_TOKEN")
        }
    }

    @Test
    fun `given accessToken is called, when sessionApi returns failure, then propagates the failure`() {
        runBlocking {
            `when`(accessTokenResponse.isSuccessful).thenReturn(false)
            `when`(accessTokenResponse.code()).thenReturn(999)
            `when`(sessionApi.accessToken(any())).thenReturn(accessTokenResponse)

            val result = sessionRemoteDataSource.accessToken(TEST_REFRESH_TOKEN)

            assertThat(result.isLeft).isTrue()
            verify(sessionApi).accessToken("$REFRESH_TOKEN_HEADER_PREFIX$TEST_REFRESH_TOKEN")
        }
    }

    companion object {
        private const val TEST_REFRESH_TOKEN = "123-refresh-token-890"
        private const val REFRESH_TOKEN_HEADER_PREFIX = "zuid="
    }
}
