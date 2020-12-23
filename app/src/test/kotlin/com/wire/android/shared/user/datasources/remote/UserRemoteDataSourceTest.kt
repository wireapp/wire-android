package com.wire.android.shared.user.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.shared.user.datasources.remote.username.ChangeHandleRequest
import com.wire.android.shared.user.datasources.remote.username.CheckHandlesExistRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
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

    @MockK
    private lateinit var usernamesResponse: Response<List<String>>

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
    fun `Given doesUsernameExist() is called, then verify request is made`() = runBlocking {
        coEvery { userApi.doesHandleExist(any()) } returns usernameResponse

        userRemoteDataSource.doesUsernameExist(TEST_USERNAME)

        coVerify(exactly = 1) { userApi.doesHandleExist(eq(TEST_USERNAME)) }
    }

    @Test
    fun `Given checkUsernamesExist() is called, then verify request is made`() {
        runBlocking {
            val listOfUsernames = listOf(TEST_USERNAME)
            val usernamesRequestSlot = slot<CheckHandlesExistRequest>()

            coEvery { userApi.checkHandlesExist(any()) } returns usernamesResponse

            userRemoteDataSource.checkUsernamesExist(listOfUsernames)

            coVerify(exactly = 1) { userApi.checkHandlesExist(capture(usernamesRequestSlot)) }
            usernamesRequestSlot.captured.handles shouldContainSame listOfUsernames
        }
    }

    @Test
    fun `Given updateUsername is called, then verify request is made`() {
        runBlocking {
            val changeUsernameRequestSlot = slot<ChangeHandleRequest>()

            coEvery { userApi.updateHandle(any()) } returns usernameResponse

            userRemoteDataSource.updateUsername(TEST_USERNAME)

            coVerify(exactly = 1) { userApi.updateHandle(capture(changeUsernameRequestSlot)) }

            changeUsernameRequestSlot.captured.handle shouldBeEqualTo TEST_USERNAME
        }
    }

    companion object {
        private const val TEST_ACCESS_TOKEN = "access-token-567"
        private const val TEST_TOKEN_TYPE = "token-type-bearer"
        private const val TEST_USERNAME = "username"
    }
}
