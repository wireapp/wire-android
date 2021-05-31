package com.wire.android.feature.auth.client.mapper

import com.wire.android.UnitTest
import com.wire.android.core.config.DeviceConfig
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.LocationResponse
import com.wire.android.shared.config.DeviceClassMapper
import com.wire.android.shared.config.DeviceTypeMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ClientMapperTest : UnitTest() {

    private lateinit var clientMapper: ClientMapper

    @MockK
    private lateinit var deviceTypeMapper: DeviceTypeMapper

    @MockK
    private lateinit var deviceClassMapper: DeviceClassMapper

    @MockK
    private lateinit var preKeyMapper: PreKeyMapper

    @MockK
    private lateinit var deviceConfig: DeviceConfig

    @Before
    fun setUp() {
        clientMapper = ClientMapper(deviceTypeMapper, deviceClassMapper, deviceConfig, preKeyMapper)
    }

    @Test
    fun `given fromClientResponseToClientEntity is called, when clientReposne model is valid, then returns correct clientEntity`() {
        val locationResponse = mockk<LocationResponse>()
        val clientResponse = mockk<ClientResponse>()
        every { clientResponse.id } returns TEST_CLIENT_ID

        val result = clientMapper.fromClientResponseToClientEntity(clientResponse)

        result.id shouldBeEqualTo TEST_CLIENT_ID

    }

    companion object {
        private const val TEST_CLIENT_ID = "test-id-123"
    }
}
