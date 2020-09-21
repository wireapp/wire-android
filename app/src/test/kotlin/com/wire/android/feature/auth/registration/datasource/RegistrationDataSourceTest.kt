package com.wire.android.feature.auth.registration.datasource

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource
import com.wire.android.feature.auth.registration.datasource.remote.RegisteredUserResponse
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class RegistrationDataSourceTest : UnitTest() {

    @Mock
    private lateinit var remoteDataSource: RegistrationRemoteDataSource

    private lateinit var registrationDataSource: RegistrationDataSource

    @Before
    fun setUp() {
        registrationDataSource = RegistrationDataSource(remoteDataSource)
    }

    @Test
    fun `given credentials, when registerPersonalAccount() is called, then calls remote data source with  credentials`() {
        runBlocking {
            val userResponse = mockUserResponse()
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Right(userResponse))

            registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            verify(remoteDataSource).registerPersonalAccount(
                name = TEST_NAME, email = TEST_EMAIL, password = TEST_PASSWORD, activationCode = TEST_ACTIVATION_CODE
            )
        }
    }

    @Test
    fun `given registerPersonalAccount() is called, when remote data source returns a UserResponse, then returns the user's id`() {
        runBlocking {
            val userResponse = mockUserResponse()
            `when`(remoteDataSource.registerPersonalAccount(any(), any(), any(), any())).thenReturn(Either.Right(userResponse))

            val result = registrationDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            result.assertRight {
                assertThat(it).isEqualTo(TEST_USER_ID)
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
        private const val TEST_USER_ID = "1234ds-dkfsdf-324"

        private fun mockUserResponse() = mock(RegisteredUserResponse::class.java).also {
            lenient().`when`(it.id).thenReturn(TEST_USER_ID)
        }
    }
}
