package com.wire.android.feature.auth.registration.datasource

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.datasource.remote.RegisteredUserResponse
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource
import com.wire.android.feature.auth.registration.personal.PersonalAccountRegistrationResult
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.session.mapper.SessionMapper
import com.wire.android.shared.user.User
import com.wire.android.shared.user.mapper.UserMapper
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import retrofit2.Response

class RegistrationDataSourceTest : UnitTest() {

    @Mock
    private lateinit var remoteDataSource: RegistrationRemoteDataSource

    @Mock
    private lateinit var userMapper: UserMapper

    @Mock
    private lateinit var sessionMapper: SessionMapper

    @Mock
    private lateinit var registeredUserResponse: Response<RegisteredUserResponse>

    @Mock
    private lateinit var headers: Headers

    @Mock
    private lateinit var registeredUserResponseBody: RegisteredUserResponse

    @Mock
    private lateinit var user: User

    private lateinit var registrationDataSource: RegistrationDataSource

    @Before
    fun setUp() {
        `when`(registeredUserResponse.body()).thenReturn(registeredUserResponseBody)
        `when`(registeredUserResponse.headers()).thenReturn(headers)
        registrationDataSource = RegistrationDataSource(remoteDataSource, userMapper, sessionMapper)
    }

    @Test
    fun `given credentials, when registerPersonalAccount() is called, then calls remote data source with credentials`() {
        runBlocking {
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Right(registeredUserResponse))

            registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            verify(remoteDataSource).registerPersonalAccount(
                name = TEST_NAME, email = TEST_EMAIL, password = TEST_PASSWORD, activationCode = TEST_ACTIVATION_CODE
            )
        }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remoteDS returns a response and mappers map successfully, then returns mapping`() {
        runBlocking {
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Right(registeredUserResponse))
            `when`(userMapper.fromRegisteredUserResponse(registeredUserResponseBody)).thenReturn(user)
            `when`(sessionMapper.extractRefreshToken(headers)).thenReturn(TEST_REFRESH_TOKEN)

            val result = registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            result.assertRight {
                assertThat(it).isEqualTo(PersonalAccountRegistrationResult(user, TEST_REFRESH_TOKEN))
            }
            verify(userMapper).fromRegisteredUserResponse(registeredUserResponseBody)
            verify(sessionMapper).extractRefreshToken(headers)
        }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remoteDS response has a null body, then returns mapping with null User`() {
        runBlocking {
            `when`(registeredUserResponse.body()).thenReturn(null)
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Right(registeredUserResponse))
            `when`(sessionMapper.extractRefreshToken(headers)).thenReturn(TEST_REFRESH_TOKEN)

            val result = registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            result.assertRight {
                assertThat(it).isEqualTo(PersonalAccountRegistrationResult(null, TEST_REFRESH_TOKEN))
            }
            verifyNoInteractions(userMapper)
            verify(sessionMapper).extractRefreshToken(headers)
        }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remoteDS response has invalid header, then returns mapping with null token`() {
        runBlocking {
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Right(registeredUserResponse))
            `when`(userMapper.fromRegisteredUserResponse(registeredUserResponseBody)).thenReturn(user)
            `when`(sessionMapper.extractRefreshToken(headers)).thenReturn(null)

            val result = registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            result.assertRight {
                assertThat(it).isEqualTo(PersonalAccountRegistrationResult(user, null))
            }
            verify(userMapper).fromRegisteredUserResponse(registeredUserResponseBody)
            verify(sessionMapper).extractRefreshToken(headers)
        }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remote data source returns failure, then returns that failure`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Left(failure))

            val result = registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verifyNoInteractions(userMapper)
            verifyNoInteractions(sessionMapper)
        }
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "abc123!"
        private const val TEST_ACTIVATION_CODE = "123456"
        private const val TEST_REFRESH_TOKEN = "refresh-token-789"
    }
}
