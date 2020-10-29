package com.wire.android.feature.auth.activation.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ActivationRemoteDataSourceTest : UnitTest() {

    private lateinit var activationRemoteDataSource: ActivationRemoteDataSource

    @MockK
    private lateinit var activationApi: ActivationApi

    @MockK
    private lateinit var response: Response<Unit>

    @Before
    fun setUp() {
        activationRemoteDataSource = ActivationRemoteDataSource(activationApi, connectedNetworkHandler)
    }

    @Test
    fun `Given an email, when sendEmailActivationCode() is called, calls activationApi with correct SendEmailActivationCodeRequest`() {
        val activationCodeRequestSlot = slot<SendEmailActivationCodeRequest>()

        runBlocking {
            activationRemoteDataSource.sendEmailActivationCode(TEST_EMAIL)

            coVerify(exactly = 1) { activationApi.sendActivationCode(capture(activationCodeRequestSlot)) }
            activationCodeRequestSlot.captured.email shouldBe TEST_EMAIL
        }
    }

    @Test
    fun `Given sendEmailActivationCode() is called, when api response is successful, then return success`() {
        every { response.body() } returns Unit
        every { response.isSuccessful } returns true

        runBlocking {
            coEvery { activationApi.sendActivationCode(any()) } returns response

            val result = activationRemoteDataSource.sendEmailActivationCode(TEST_EMAIL)

            coVerify(exactly = 1) { activationApi.sendActivationCode(any()) }
            result shouldSucceed {}
        }
    }

    @Test
    fun `Given sendEmailActivationCode() is called, when api response fails, then return a failure`() {
        every { response.isSuccessful } returns false

        runBlocking {
            coEvery { activationApi.sendActivationCode(any()) } returns response

            val result = activationRemoteDataSource.sendEmailActivationCode(TEST_EMAIL)

            coVerify(exactly = 1) { activationApi.sendActivationCode(any()) }
            result.isLeft shouldBe true
        }
    }

    @Test
    fun `Given an email and a code, when activateEmail() is called, calls activationApi with correct EmailActivationRequest`() {
        val activationRequestSlot = slot<EmailActivationRequest>()

        runBlocking {
            activationRemoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE)

            coVerify(exactly = 1) { activationApi.activateEmail(capture(activationRequestSlot)) }

            with(activationRequestSlot.captured) {
                email shouldBe TEST_EMAIL
                code shouldBe TEST_CODE
                dryrun shouldBe true
            }
        }
    }

    @Test
    fun `Given activateEmail() is called, when api response is successful, then return success`() {
        every { response.body() } returns Unit
        every { response.isSuccessful } returns true

        runBlocking {
            coEvery { activationApi.activateEmail(any()) } returns response

            val result = activationRemoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE)

            coVerify(exactly = 1) { activationApi.activateEmail(any()) }
            result shouldSucceed {}
        }
    }

    @Test
    fun `Given activateEmail() is called, when api response fails, then return failure`() {
        every { response.isSuccessful } returns false

        runBlocking {
            coEvery { activationApi.activateEmail(any()) } returns response

            val result = activationRemoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE)

            coVerify(exactly = 1) { activationApi.activateEmail(any()) }
            result.isLeft shouldBe true
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_CODE = "123456"
    }
}
