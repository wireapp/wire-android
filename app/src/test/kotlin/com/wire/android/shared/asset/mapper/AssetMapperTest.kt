package com.wire.android.shared.asset.mapper

import com.wire.android.UnitTest
import com.wire.android.shared.asset.datasources.local.AssetEntity
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import io.mockk.every
import io.mockk.mockk
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
        val assetResponse = mockk<AssetResponse>().also {
            every { it.size } returns TEST_SIZE_INVALID
        }

        val result = assetMapper.profilePictureAssetKey(listOf(assetResponse))

        result.shouldBeNull()
    }

    @Test
    fun `given profilePictureAssetKey is called, when asset list contains an asset with complete type, then return asset key`() {
        val assetResponse1 = mockk<AssetResponse>().also {
            every { it.size } returns TEST_SIZE
            every { it.key } returns TEST_KEY
        }

        val assetResponse2 = mockk<AssetResponse>().also {
            every { it.size } returns TEST_SIZE_INVALID
        }

        val result = assetMapper.profilePictureAssetKey(listOf(assetResponse1, assetResponse2))

        result shouldBeEqualTo TEST_KEY
    }

    companion object {
        private const val TEST_SIZE = "complete"
        private const val TEST_KEY = "35dc-1239ac-jxq"
        private const val TEST_TYPE = "image"

        private const val TEST_SIZE_INVALID = "invalid_size"
    }
}
