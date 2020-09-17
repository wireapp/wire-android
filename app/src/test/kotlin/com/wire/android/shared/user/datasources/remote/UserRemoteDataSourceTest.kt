package com.wire.android.shared.user.datasources.remote

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

class UserRemoteDataSourceTest : UnitTest() {

    @Mock
    private lateinit var userApi: UserApi

    @Mock
    private lateinit var selfUserResponse: Response<SelfUserResponse>

    @Mock
    private lateinit var selfUserResponseBody: SelfUserResponse

    private lateinit var userRemoteDataSource: UserRemoteDataSource

    @Before
    fun setUp() {
        userRemoteDataSource = UserRemoteDataSource(connectedNetworkHandler, userApi)
    }

    @Test
    fun `given selfUser is called with access token and token type, then calls userApi with correct auth header`() {
        runBlocking {
            `when`(selfUserResponse.isSuccessful).thenReturn(true)
            `when`(selfUserResponse.body()).thenReturn(selfUserResponseBody)
            `when`(userApi.selfUser(any())).thenReturn(selfUserResponse)

            userRemoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)

            verify(userApi).selfUser("$TEST_TOKEN_TYPE $TEST_ACCESS_TOKEN")
        }
    }

    @Test
    fun `given selfUser is called, when userApi returns response, then returns the response body as success`() {
        runBlocking {
            `when`(selfUserResponse.isSuccessful).thenReturn(true)
            `when`(selfUserResponse.body()).thenReturn(selfUserResponseBody)
            `when`(userApi.selfUser(any())).thenReturn(selfUserResponse)

            val result = userRemoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)

            result.assertRight {
                assertThat(it).isEqualTo(selfUserResponseBody)
            }
        }
    }

    @Test
    fun `given selfUser is called, when userApi returns failure, then returns the failure`() {
        runBlocking {
            `when`(selfUserResponse.isSuccessful).thenReturn(false)
            `when`(selfUserResponse.code()).thenReturn(999)
            `when`(userApi.selfUser(any())).thenReturn(selfUserResponse)

            val result = userRemoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)

            assertThat(result.isLeft).isTrue()
        }
    }

    companion object {
        private const val TEST_ACCESS_TOKEN = "access-token-567"
        private const val TEST_TOKEN_TYPE = "token-type-bearer"
    }
}
