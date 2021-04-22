package com.wire.android.shared.asset.mapper

import com.wire.android.UnitTest
import com.wire.android.shared.asset.datasources.local.AssetEntity
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
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

    @Test
    fun `given profilePictureAssetKey is called, when asset list is empty, then return null`() {
        val assets = listOf<AssetResponse>()

        val result = assetMapper.profilePictureAssetKey(assets)

        result.shouldBeNull()
    }

    @Test
    fun `given profilePictureAssetKey is called, when assets list does not contain completed size picture, then return null`() {
        val assets = listOf(
            AssetResponse(TEST_SIZE_UNKNOWN, TEST_KEY_UNKNOWN, TEST_TYPE_UNKNOWN),
            AssetResponse(
                TEST_SIZE + TEST_SIZE_UNKNOWN,
                TEST_KEY + TEST_KEY_UNKNOWN,
                TEST_TYPE + TEST_TYPE_UNKNOWN
            )
        )

        val result = assetMapper.profilePictureAssetKey(assets)

        result.shouldBeNull()
    }

    @Test
    fun `given profilePictureAssetKey is called, when asset list contains an asset with complete type, then return asset key`() {
        val assets = listOf(
            AssetResponse(TEST_SIZE, TEST_KEY, TEST_TYPE),
            AssetResponse(TEST_SIZE_UNKNOWN, TEST_KEY_UNKNOWN, TEST_TYPE_UNKNOWN)
        )

        val result = assetMapper.profilePictureAssetKey(assets)

        result shouldBeEqualTo TEST_KEY
    }

    companion object {
        private const val TEST_SIZE = "complete"
        private const val TEST_KEY = "35dc-1239ac-jxq"
        private const val TEST_TYPE = "image"

        private const val TEST_SIZE_UNKNOWN = "unknown"
        private const val TEST_KEY_UNKNOWN = "unknown"
        private const val TEST_TYPE_UNKNOWN = "unknown"
    }
}
