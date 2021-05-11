package com.wire.android.shared.config

import com.wire.android.UnitTest
import com.wire.android.core.config.Phone
import com.wire.android.core.config.Tablet
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class DeviceClassMapperTest : UnitTest() {

    private lateinit var deviceTypeMapper: DeviceTypeMapper

    @Before
    fun setUp() {
        deviceTypeMapper = DeviceTypeMapper()
    }

    @Test
    fun `given toStringValue() is called, when type is Tablet, then return tablet string value`() {
        val result = deviceTypeMapper.toStringValue(Tablet)
        result shouldBeEqualTo "tablet"
    }

    @Test
    fun `given toStringValue() is called, when type is Phone, then return phone string value`() {
        val result = deviceTypeMapper.toStringValue(Phone)
        result shouldBeEqualTo "phone"
    }

}
