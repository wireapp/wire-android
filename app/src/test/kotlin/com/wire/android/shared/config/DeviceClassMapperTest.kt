package com.wire.android.shared.config

import com.wire.android.UnitTest
import com.wire.android.core.config.Phone
import com.wire.android.core.config.Tablet
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class DeviceClassMapperTest : UnitTest() {

    private lateinit var deviceClassMapper: DeviceClassMapper

    @Before
    fun setUp() {
        deviceClassMapper = DeviceClassMapper()
    }

    @Test
    fun `given toStringValue is called, when type is Tablet, then return tablet string value`() {
        val result = deviceClassMapper.toStringValue(Tablet)
        result shouldBeEqualTo "tablet"
    }

    @Test
    fun `given toStringValue is called, when type is Phone, then return phone string value`() {
        val result = deviceClassMapper.toStringValue(Phone)
        result shouldBeEqualTo "phone"
    }

}
