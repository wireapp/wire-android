package com.wire.android.feature.auth.client.datasource

import com.wire.android.UnitTest
import com.wire.android.core.config.DeviceConfig
import com.wire.android.core.config.Permanent
import com.wire.android.core.config.Phone
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ClientDataSourceTest : UnitTest() {

    @MockK
    private lateinit var cryptoBoxClient: CryptoBoxClient

    @MockK
    private lateinit var deviceConfig: DeviceConfig

    private lateinit var clientDataSource: ClientDataSource

    @Before
    fun setUp() {
        clientDataSource =
            ClientDataSource(cryptoBoxClient, deviceConfig)
    }

    @Test
    fun `given createNewClient is called, when client is successfully created, then return a valid client`() {
        val deviceName = "Wire-device"
        val deviceModelName = "Google Pixel 4"
        val keys= mockk<List<PreKey>>()
        val preKey = mockk<PreKey>()
        val preKeyInitialization = mockk<PreKeyInitialization>().also {
            every { it.createdKeys } returns keys
            every { it.lastKey } returns preKey
        }
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { deviceConfig.deviceName() } returns deviceName
        every { deviceConfig.deviceClass() } returns Phone
        every { deviceConfig.deviceModelName() } returns deviceModelName

        val result = runBlocking { clientDataSource.createNewClient(USER_ID, PASSWORD) }

        result.shouldSucceed {
            it.id shouldBeEqualTo USER_ID
            it.password shouldBeEqualTo PASSWORD
            it.label shouldBeEqualTo deviceName
            it.deviceClass shouldBeEqualTo Phone
            it.deviceType shouldBeEqualTo Permanent
            it.lastKey shouldBeEqualTo preKey
            it.preKeys shouldBeEqualTo keys
            it.model shouldBeEqualTo deviceModelName
        }

    }

    @Test
    fun `given createNewClient is called, when preKeys creation fails, then return CryptoBoxFailure`() {
        val failure = mockk<CryptoBoxFailure>()

        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Left(failure)

        val result = runBlocking { clientDataSource.createNewClient(USER_ID, PASSWORD) }

        result.shouldFail { it shouldBeEqualTo failure }
    }

    companion object{
        private const val USER_ID = "user-id"
        private const val PASSWORD = "password"
    }
}
