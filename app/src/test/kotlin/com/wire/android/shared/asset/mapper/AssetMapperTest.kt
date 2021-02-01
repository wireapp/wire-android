package com.wire.android.shared.asset.mapper

import com.wire.android.UnitTest
import com.wire.android.shared.asset.datasources.local.AssetEntity
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class AssetMapperTest : UnitTest() {

    private lateinit var assetMapper: AssetMapper

    @Before
    fun setUp() {
        assetMapper = AssetMapper()
    }

    @Test
    fun `given fromResponseToEntity is called, then maps given response to entity`() {
        val assetResponse = AssetResponse(size = TEST_SIZE, key = TEST_KEY, type = TEST_TYPE)

        val result = assetMapper.fromResponseToEntity(assetResponse)

        result.let {
            it shouldBeInstanceOf AssetEntity::class
            it.size shouldBeEqualTo TEST_SIZE
            it.key shouldBeEqualTo TEST_KEY
            it.type shouldBeEqualTo TEST_TYPE
        }
    }

    companion object {
        private const val TEST_SIZE = "complete"
        private const val TEST_KEY = "35dc-1239ac-jxq"
        private const val TEST_TYPE = "image"
    }
}
