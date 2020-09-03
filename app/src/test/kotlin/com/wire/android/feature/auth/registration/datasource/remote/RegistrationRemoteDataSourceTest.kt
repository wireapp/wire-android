package com.wire.android.feature.auth.registration.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.capture
import com.wire.android.core.locale.LocaleConfig
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.shared.auth.remote.LabelGenerator
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import retrofit2.Response
import java.util.Locale

class RegistrationRemoteDataSourceTest : UnitTest() {

    @Mock
    private lateinit var api: RegistrationApi

    @Mock
    private lateinit var labelGenerator: LabelGenerator

    @Mock
    private lateinit var localeConfig: LocaleConfig

    @Captor
    private lateinit var registerPersonalAccountWithEmailRequestCaptor: ArgumentCaptor<RegisterPersonalAccountWithEmailRequest>

    private lateinit var remoteDataSource: RegistrationRemoteDataSource

    @Before
    fun setUp() {
        mockLocale().let { `when`(localeConfig.currentLocale()).thenReturn(it) }
        `when`(labelGenerator.newLabel()).thenReturn(TEST_LABEL)
        remoteDataSource = RegistrationRemoteDataSource(api, labelGenerator, localeConfig, connectedNetworkHandler)
    }

    @Test
    fun `given credentials, when registerPersonalAccountWithEmail() is called, calls the api with correct params`() {
        runBlocking {
            mockUserResponse().let { `when`(api.register(any())).thenReturn(it) }

            remoteDataSource.registerPersonalAccountWithEmail(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            verify(api).register(capture(registerPersonalAccountWithEmailRequestCaptor))
            registerPersonalAccountWithEmailRequestCaptor.value.let {
                assertThat(it.name).isEqualTo(TEST_NAME)
                assertThat(it.email).isEqualTo(TEST_EMAIL)
                assertThat(it.password).isEqualTo(TEST_PASSWORD)
                assertThat(it.emailCode).isEqualTo(TEST_ACTIVATION_CODE)
                assertThat(it.locale).isEqualTo(TEST_LOCALE)
                assertThat(it.label).isEqualTo(TEST_LABEL)
            }
        }
    }

    @Test
    fun `given registerPersonalAccountWithEmail() is called, when api returns UserResponse, returns success with UserResponse`() {
        runBlocking {
            val userResponse = mock(UserResponse::class.java)
            mockUserResponse().let {
                `when`(it.body()).thenReturn(userResponse)
                `when`(api.register(any())).thenReturn(it)
            }

            val result = remoteDataSource.registerPersonalAccountWithEmail(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            result.assertRight {
                assertThat(it).isEqualTo(userResponse)
            }
        }
    }

    @Test
    fun `given registerPersonalAccountWithEmail() is called, when api returns error, returns failure`() {
        runBlocking {
            val response = mockUserResponse(successful = false)
            `when`(response.code()).thenReturn(400)

            `when`(api.register(any())).thenReturn(response)

            val result = remoteDataSource.registerPersonalAccountWithEmail(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)

            assertThat(result.isLeft).isTrue()
        }
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "abc123!"
        private const val TEST_ACTIVATION_CODE = "123456"
        private const val TEST_LOCALE = "en-US"
        private const val TEST_LABEL = "label"

        private fun mockLocale(): Locale = mock(Locale::class.java).also {
            `when`(it.toLanguageTag()).thenReturn(TEST_LOCALE)
        }

        @Suppress("UNCHECKED_CAST")
        private fun mockUserResponse(successful: Boolean = true): Response<UserResponse> =
            (mock(Response::class.java) as Response<UserResponse>).also {
                `when`(it.isSuccessful).thenReturn(successful)
            }
    }
}
