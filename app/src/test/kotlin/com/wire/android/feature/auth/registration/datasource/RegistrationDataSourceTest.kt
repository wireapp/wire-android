package com.wire.android.feature.auth.registration.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.datasource.remote.RegisteredUserResponse
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource
import com.wire.android.feature.auth.registration.personal.PersonalAccountRegistrationResult
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.mapper.SessionMapper
import com.wire.android.shared.user.User
import com.wire.android.shared.user.mapper.UserMapper
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class RegistrationDataSourceTest : UnitTest() {

    @MockK
    private lateinit var remoteDataSource: RegistrationRemoteDataSource

    @MockK
    private lateinit var userMapper: UserMapper

    @MockK
    private lateinit var sessionMapper: SessionMapper

    @MockK
    private lateinit var registeredUserResponse: Response<RegisteredUserResponse>

    @MockK
    private lateinit var headers: Headers

    @MockK
    private lateinit var registeredUserResponseBody: RegisteredUserResponse

    @MockK
    private lateinit var user: User

    private lateinit var registrationDataSource: RegistrationDataSource

    @Before
    fun setUp() {
        every { registeredUserResponse.body() } returns registeredUserResponseBody
        every { registeredUserResponse.headers() } returns headers
        registrationDataSource = RegistrationDataSource(remoteDataSource, userMapper, sessionMapper)
    }

    @Test
    fun `given credentials, when registerPersonalAccount() is called, then calls remote data source with credentials`() {
        coEvery { remoteDataSource.registerPersonalAccount(any(), any(), any(), any(), any()) } returns Either.Right(registeredUserResponse)

        runBlocking {
            registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
        }

        coVerify(exactly = 1) {
            remoteDataSource.registerPersonalAccount(
                name = TEST_NAME, email = TEST_EMAIL, username = TEST_USERNAME, password = TEST_PASSWORD, activationCode = TEST_ACTIVATION_CODE
            )
        }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remoteDS returns a response and mappers map successfully, then returns mapping`() {
        coEvery { remoteDataSource.registerPersonalAccount(any(), any(), any(), any(), any()) } returns Either.Right(registeredUserResponse)
        every { userMapper.fromRegisteredUserResponse(registeredUserResponseBody) } returns user
        every { sessionMapper.extractRefreshToken(headers) } returns TEST_REFRESH_TOKEN

        val result = runBlocking {
            registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
        }

        result shouldSucceed { it shouldBeEqualTo PersonalAccountRegistrationResult(user, TEST_REFRESH_TOKEN) }
        verify(exactly = 1) { userMapper.fromRegisteredUserResponse(registeredUserResponseBody) }
        verify(exactly = 1) { sessionMapper.extractRefreshToken(headers) }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remoteDS response has a null body, then returns mapping with null User`() {
        every { registeredUserResponse.body() } returns null
        coEvery { remoteDataSource.registerPersonalAccount(any(), any(), any(), any(), any()) } returns Either.Right(registeredUserResponse)
        every { sessionMapper.extractRefreshToken(headers) } returns TEST_REFRESH_TOKEN

        val result = runBlocking {
            registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
        }

        result shouldSucceed { it shouldBeEqualTo PersonalAccountRegistrationResult(null, TEST_REFRESH_TOKEN) }
        verify { userMapper wasNot Called }
        verify(exactly = 1) { sessionMapper.extractRefreshToken(headers) }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remoteDS response has invalid header, then returns mapping with null token`() {
        coEvery { remoteDataSource.registerPersonalAccount(any(), any(), any(), any(), any()) } returns Either.Right(registeredUserResponse)
        every { userMapper.fromRegisteredUserResponse(registeredUserResponseBody) } returns user
        every { sessionMapper.extractRefreshToken(headers) } returns null

        val result = runBlocking {
            registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
        }

        result shouldSucceed { it shouldBeEqualTo PersonalAccountRegistrationResult(user, null) }
        verify(exactly = 1) { userMapper.fromRegisteredUserResponse(registeredUserResponseBody) }
        verify(exactly = 1) { sessionMapper.extractRefreshToken(headers) }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remote data source returns failure, then returns that failure`() {
        val failure = mockk<Failure>()
        coEvery { remoteDataSource.registerPersonalAccount(any(), any(), any(), any(), any()) } returns Either.Left(failure)

        val result = runBlocking {
            registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
        }

        result shouldFail { it shouldBe failure }
        verify { userMapper wasNot Called }
        verify { sessionMapper wasNot Called }
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_USERNAME = "username"
        private const val TEST_PASSWORD = "abc123!"
        private const val TEST_ACTIVATION_CODE = "123456"
        private const val TEST_REFRESH_TOKEN = "refresh-token-789"
    }
}
