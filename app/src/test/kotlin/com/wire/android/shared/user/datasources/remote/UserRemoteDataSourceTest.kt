package com.wire.android.shared.user.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.shared.user.username.UsernameGeneralError
import com.wire.android.shared.user.username.UsernameAlreadyExists
import com.wire.android.shared.user.username.UsernameInvalid
import com.wire.android.shared.user.username.UsernameIsAvailable
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class UserRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var userApi: UserApi

    @MockK
    private lateinit var selfUserResponse: Response<SelfUserResponse>

    @MockK
    private lateinit var selfUserResponseBody: SelfUserResponse

    @MockK
    private lateinit var usernameResponse: Response<Unit>

    private lateinit var userRemoteDataSource: UserRemoteDataSource

    @Before
    fun setUp() {
        userRemoteDataSource = UserRemoteDataSource(connectedNetworkHandler, userApi)
    }

    @Test
    fun `given selfUser is called with access token and token type, then calls userApi with correct auth header`() {
        every { selfUserResponse.isSuccessful } returns true
        every { selfUserResponse.body() } returns selfUserResponseBody
        coEvery { userApi.selfUser(any()) } returns selfUserResponse

        runBlocking {
            userRemoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)
        }

        coVerify(exactly = 1) { userApi.selfUser("$TEST_TOKEN_TYPE $TEST_ACCESS_TOKEN") }
    }

    @Test
    fun `given selfUser is called, when userApi returns response, then returns the response body as success`() {
        every { selfUserResponse.isSuccessful } returns true
        every { selfUserResponse.body() } returns selfUserResponseBody
        coEvery { userApi.selfUser(any()) } returns selfUserResponse

        val result = runBlocking {
            userRemoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)
        }

        result shouldSucceed { it shouldBe selfUserResponseBody }
    }

    @Test
    fun `given selfUser is called, when userApi returns failure, then returns the failure`() {
        every { selfUserResponse.isSuccessful } returns false
        every { selfUserResponse.code() } returns 999
        coEvery { userApi.selfUser(any()) } returns selfUserResponse

        val result = runBlocking {
            userRemoteDataSource.selfUser(accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE)
        }

        result shouldFail {}
    }

    @Test
    fun `Given doesUsernameExist() is called, when response code is 404, then propagate UsernameIsAvailable success`() = runBlocking {
        every { selfUserResponse.isSuccessful } returns false
        every { usernameResponse.code() } returns USERNAME_AVAILABLE
        coEvery { userApi.doesHandleExist(any()) } returns usernameResponse

        val response = userRemoteDataSource.doesUsernameExist(TEST_USERNAME)

        coVerify { userApi.doesHandleExist(eq(TEST_USERNAME)) }

        response shouldSucceed { it shouldBe UsernameIsAvailable }
    }

    @Test
    fun `Given doesUsernameExist() is called, when response code is 200, then propagate a UsernameAlreadyExists failure`() {
        every { usernameResponse.code() } returns USERNAME_TAKEN

        validateUsernameExistsFailure(failure = UsernameAlreadyExists)

    }

    @Test
    fun `Given doesUsernameExist() is called, when response code is 400, then propagate a UsernameInvalid failure`() {
        every { usernameResponse.code() } returns USERNAME_INVALID

        validateUsernameExistsFailure(failure = UsernameInvalid)

    }

    @Test
    fun `Given doesUsernameExist() is called, when response code is not 200, 400 or 404, then propagate a UnknownUsernameError failure`() {
        every { usernameResponse.code() } returns USERNAME_UNKNOWN

        validateUsernameExistsFailure(failure = UsernameGeneralError)
    }

    private fun validateUsernameExistsFailure(failure: Failure) = runBlocking {
        every { selfUserResponse.isSuccessful } returns false
        coEvery { userApi.doesHandleExist(any()) } returns usernameResponse

        val response = userRemoteDataSource.doesUsernameExist(TEST_USERNAME)

        coVerify { userApi.doesHandleExist(eq(TEST_USERNAME)) }

        response shouldFail { it shouldBe failure }
    }

    companion object {
        private const val TEST_ACCESS_TOKEN = "access-token-567"
        private const val TEST_TOKEN_TYPE = "token-type-bearer"

        private const val TEST_USERNAME = "username"
        private const val USERNAME_TAKEN = 200
        private const val USERNAME_INVALID = 400
        private const val USERNAME_AVAILABLE = 404
        private const val USERNAME_UNKNOWN = 500
    }
}
