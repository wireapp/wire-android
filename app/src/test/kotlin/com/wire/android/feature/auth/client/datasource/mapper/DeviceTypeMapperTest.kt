package com.wire.android.feature.auth.client.datasource.mapper

import com.wire.android.UnitTest
import com.wire.android.core.config.LegalHold
import com.wire.android.core.config.Permanent
import com.wire.android.core.config.Temporary
import com.wire.android.core.config.Unknown
import com.wire.android.shared.config.DeviceTypeMapper
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class DeviceTypeMapperTest : UnitTest() {

    private lateinit var deviceTypeMapper: DeviceTypeMapper

    @Before
    fun setUp() {
        deviceTypeMapper = DeviceTypeMapper()
    }

    @Test
    fun `given value is called, when client type is Permanent, then return permanent string value`() {
        val result = deviceTypeMapper.value(Permanent)
        result shouldBeEqualTo "permanent"
    }

    @Test
    fun `given value is called, when client type is Temporary, then return temporary string value`() {
        val result = deviceTypeMapper.value(Temporary)
        result shouldBeEqualTo "temporary"
    }

    @Test
    fun `given value is called, when client type is LegalHold, then return legalhold string value`() {
        val result = deviceTypeMapper.value(LegalHold)
        result shouldBeEqualTo "legalhold"
    }

    @Test
    fun `given value is called, when client type is Unknown, then return unknown string value`() {
        val result = deviceTypeMapper.value(Unknown)
        result shouldBeEqualTo "unknown"
    }
}
