package com.wire.android.feature.auth.activation.datasource

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.datasource.remote.ActivationRemoteDataSource
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ActivationDataSourceTest : UnitTest() {

    private lateinit var activationDataSource: ActivationDataSource

    @MockK
    private lateinit var remoteDataSource: ActivationRemoteDataSource

    @Before
    fun setUp() {
        activationDataSource = ActivationDataSource(remoteDataSource)
    }

    @Test
    fun `Given sendEmailActivationCode() is called and remote request fails then return failure`() {
        coEvery { remoteDataSource.sendEmailActivationCode(TEST_EMAIL) } returns Either.Left(ServerError)

        val response = runBlocking { activationDataSource.sendEmailActivationCode(TEST_EMAIL) }

        coVerify(exactly = 1) { remoteDataSource.sendEmailActivationCode(TEST_EMAIL) }
        response shouldFail { it shouldBe ServerError }
    }

    @Test
    fun `Given sendEmailActivationCode() is called and remote request is success, then return success`() {
        coEvery { remoteDataSource.sendEmailActivationCode(TEST_EMAIL) } returns Either.Right(Unit)

        val response = runBlocking { activationDataSource.sendEmailActivationCode(TEST_EMAIL) }

        coVerify(exactly = 1) { remoteDataSource.sendEmailActivationCode(TEST_EMAIL) }
        response shouldSucceed {}
    }

    @Test
    fun `Given activateEmail() is called and remote request fails then return failure`() {
        coEvery { remoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE) } returns Either.Left(ServerError)

        val response = runBlocking { activationDataSource.activateEmail(TEST_EMAIL, TEST_CODE) }

        coVerify(exactly = 1) { remoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE) }
        response shouldFail { it shouldBe ServerError }
    }

    @Test
    fun `Given activateEmail() is called and remote request success then return success`() {
        coEvery { remoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE) } returns Either.Right(Unit)

        val response = runBlocking { activationDataSource.activateEmail(TEST_EMAIL, TEST_CODE) }

        coVerify(exactly = 1) { remoteDataSource.activateEmail(TEST_EMAIL, TEST_CODE) }
        response shouldSucceed {}
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_CODE = "123456"
    }
}
