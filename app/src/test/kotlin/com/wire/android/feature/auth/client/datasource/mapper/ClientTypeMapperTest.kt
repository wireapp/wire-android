package com.wire.android.feature.auth.client.datasource.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.auth.client.datasource.LegalHold
import com.wire.android.feature.auth.client.datasource.Permanent
import com.wire.android.feature.auth.client.datasource.Temporary
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ClientTypeMapperTest : UnitTest() {

    private lateinit var clientTypeMapper: ClientTypeMapper

    @Before
    fun setUp() {
        clientTypeMapper = ClientTypeMapper()
    }

    @Test
    fun `given toStringValue() is called, when client type is Permanent, then return permanent string value`() {
        val result = clientTypeMapper.toStringValue(Permanent)
        result shouldBeEqualTo "permanent"
    }

    @Test
    fun `given toStringValue() is called, when client type is Temporary, then return temporary string value`() {
        val result = clientTypeMapper.toStringValue(Temporary)
        result shouldBeEqualTo "temporary"
    }

    @Test
    fun `given toStringValue() is called, when client type is LegalHold, then return legalhold string value`() {
        val result = clientTypeMapper.toStringValue(LegalHold)
        result shouldBeEqualTo "legalhold"
    }
}
