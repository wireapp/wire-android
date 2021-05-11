package com.wire.android.feature.auth.client.datasource

import android.os.Build
import com.wire.android.UnitTest
import com.wire.android.core.config.DeviceConfig
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
import java.lang.reflect.Field
import java.lang.reflect.Modifier

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
        val userId = "user-id"
        val password = "user-id"
        val deviceName = "Wire-device"
        val manufacturer = "Google"
        val model = "Pixel 4"

        val preKey = mockk<PreKey>()
        val preKeyInitialization = mockk<PreKeyInitialization>().also {
            every { it.createdKeys } returns listOf()
            every { it.lastKey } returns preKey
        }
        mockBuild("MANUFACTURER", manufacturer)
        mockBuild("MODEL", model)
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { deviceConfig.deviceName() } returns deviceName
        every { deviceConfig.deviceClass() } returns Phone

        val result = runBlocking { clientDataSource.createNewClient(userId, password) }

        result.shouldSucceed {
            it.id shouldBeEqualTo userId
            it.password shouldBeEqualTo password
            it.label shouldBeEqualTo deviceName
            it.deviceClass shouldBeEqualTo Phone
            it.deviceType shouldBeEqualTo Permanent
            it.lastKey shouldBeEqualTo preKey
            it.preKeys shouldBeEqualTo listOf()
            it.model shouldBeEqualTo "$manufacturer $model"
        }

    }

    @Test
    fun `given createNewClient is called, when preKeys creation fails, then return CryptoBoxFailure`() {
        val failure = mockk<CryptoBoxFailure>()
        val userId = "user-id"
        val password = "user-id"
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Left(failure)

        val result = runBlocking { clientDataSource.createNewClient(userId, password) }

        result.shouldFail { it shouldBeEqualTo failure }
    }

    private fun mockBuild(name: String, value: String) {
        val sdkIntField = Build::class.java.getField(name)
        sdkIntField.isAccessible = true
        Field::class.java.getDeclaredField("modifiers").also {
            it.isAccessible = true
            it.set(sdkIntField, sdkIntField.modifiers and Modifier.FINAL.inv())
        }
        sdkIntField.set(null, value)
    }
}
