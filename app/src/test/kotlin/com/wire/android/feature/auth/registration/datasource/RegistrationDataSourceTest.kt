package com.wire.android.feature.auth.registration.datasource

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.datasource.remote.RegisteredUserResponse
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.user.User
import com.wire.android.shared.user.mapper.UserMapper
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class RegistrationDataSourceTest : UnitTest() {

    @Mock
    private lateinit var remoteDataSource: RegistrationRemoteDataSource

    @Mock
    private lateinit var userMapper: UserMapper

    @Mock
    private lateinit var registeredUserResponse: RegisteredUserResponse

    @Mock
    private lateinit var user: User

    private lateinit var registrationDataSource: RegistrationDataSource

    @Before
    fun setUp() {
        registrationDataSource = RegistrationDataSource(remoteDataSource, userMapper)
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
    fun `given registerPersonalAccount() is called, when remote data source returns a response, then maps it into user and returns it`() {
        runBlocking {
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Right(registeredUserResponse))
            `when`(userMapper.fromRegisteredUserResponse(registeredUserResponse)).thenReturn(user)

            val result = registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            result.assertRight {
                assertThat(it).isEqualTo(user)
            }
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
        }
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "abc123!"
        private const val TEST_ACTIVATION_CODE = "123456"
    }
}
