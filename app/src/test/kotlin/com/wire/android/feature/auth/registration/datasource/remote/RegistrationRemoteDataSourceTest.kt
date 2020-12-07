package com.wire.android.feature.auth.registration.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.core.config.LocaleConfig
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.shared.auth.remote.LabelGenerator
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.Locale

class RegistrationRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var api: RegistrationApi

    @MockK
    private lateinit var labelGenerator: LabelGenerator

    @MockK
    private lateinit var localeConfig: LocaleConfig

    private lateinit var registerPersonalAccountRequestCaptor: CapturingSlot<RegisterPersonalAccountRequest>

    private lateinit var remoteDataSource: RegistrationRemoteDataSource

    @Before
    fun setUp() {
        registerPersonalAccountRequestCaptor = slot()
        mockLocale().let {
            every { localeConfig.currentLocale() } returns it
        }
        every { labelGenerator.newLabel() } returns TEST_LABEL
        remoteDataSource = RegistrationRemoteDataSource(api, labelGenerator, localeConfig, connectedNetworkHandler)
    }

    @Test
    fun `given credentials, when registerPersonalAccount() is called, calls the api with correct params`() {
        mockUserResponse().let {
            coEvery { api.registerPersonalAccount(any()) } returns it
        }

        runBlocking { remoteDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE) }

        coVerify(exactly = 1) { api.registerPersonalAccount(capture(registerPersonalAccountRequestCaptor)) }
        with(registerPersonalAccountRequestCaptor.captured) {
            name shouldBeEqualTo TEST_NAME
            email shouldBeEqualTo TEST_EMAIL
            password shouldBeEqualTo TEST_PASSWORD
            emailCode shouldBeEqualTo TEST_ACTIVATION_CODE
            handle shouldBeEqualTo TEST_USERNAME
            locale shouldBeEqualTo TEST_LOCALE
            label shouldBeEqualTo TEST_LABEL
        }
    }

    @Test
    fun `given registerPersonalAccount() is called, when api returns UserResponse, returns success with UserResponse body`() {
        val userResponse = mockk<RegisteredUserResponse>()
        mockUserResponse().let {
            every { it.body() } returns userResponse
            coEvery { api.registerPersonalAccount(any()) } returns it
        }

        val result = runBlocking {
            remoteDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
        }

        result shouldSucceed { it.body() shouldBeEqualTo userResponse }
    }

    @Test
    fun `given registerPersonalAccount() is called, when api returns error, returns failure`() {
        val response = mockUserResponse(successful = false)
        every { response.code() } returns 400
        coEvery { api.registerPersonalAccount(any()) } returns response

        val result = runBlocking {
            remoteDataSource.registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
        }

        result shouldFail {}
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_PASSWORD = "abc123!"
        private const val TEST_USERNAME = "username"
        private const val TEST_ACTIVATION_CODE = "123456"
        private const val TEST_LOCALE = "en-US"
        private const val TEST_LABEL = "label"

        private fun mockLocale(): Locale = mockk<Locale>().also {
            every { it.toLanguageTag() } returns TEST_LOCALE
        }

        @Suppress("UNCHECKED_CAST")
        private fun mockUserResponse(successful: Boolean = true): Response<RegisteredUserResponse> =
            (mockkClass(Response::class) as Response<RegisteredUserResponse>).also {
                every { it.isSuccessful } returns successful
            }
    }
}
