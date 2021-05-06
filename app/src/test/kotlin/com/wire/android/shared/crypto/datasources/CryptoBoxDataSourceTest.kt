package com.wire.android.shared.crypto.datasources

import com.wire.android.UnitTest
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class CryptoBoxDataSourceTest : UnitTest() {

    @MockK
    private lateinit var cryptoBoxClient: CryptoBoxClient

    private lateinit var cryptoBoxDataSource: CryptoBoxDataSource

    @Before
    fun setUp() {
        cryptoBoxDataSource = CryptoBoxDataSource(cryptoBoxClient)
    }

    @Test
    fun `given generatePreKeys is called, when preKeys generation fails, then return propagate failure`() {
        val failure = mockk<CryptoBoxFailure>()
        coEvery { cryptoBoxDataSource.generatePreKeys() } returns Either.Left(failure)

        val result = runBlocking { cryptoBoxDataSource.generatePreKeys() }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given generatePreKeys is called, when preKeys generation is successfully done, then return preKey response`() {
        val preKeyInitialization = mockk<PreKeyInitialization>()
        coEvery { cryptoBoxDataSource.generatePreKeys() } returns Either.Right(preKeyInitialization)

        val result = runBlocking { cryptoBoxDataSource.generatePreKeys() }

        result shouldSucceed { it shouldBeEqualTo preKeyInitialization }
    }
}
