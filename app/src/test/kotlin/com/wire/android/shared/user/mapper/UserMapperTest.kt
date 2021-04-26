package com.wire.android.shared.user.mapper

import com.wire.android.UnitTest
import com.wire.android.shared.asset.Asset
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import com.wire.android.shared.asset.mapper.AssetMapper
import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.remote.SelfUserResponse
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.junit.Before
import org.junit.Test

class UserMapperTest : UnitTest() {

    private lateinit var userMapper: UserMapper

    @MockK
    private lateinit var assetMapper: AssetMapper

    @Before
    fun setUp() {
        userMapper = UserMapper(assetMapper)
    }

    @Test
    fun `given fromSelfUserResponse is called with a response model, then returns correct user`() {
        val assetResponse = mockk<AssetResponse>()
        val assets = listOf(assetResponse)
        val selfUserResponse = SelfUserResponse(
            id = TEST_USER_ID, name = TEST_USER_NAME,
            email = TEST_EMAIL, locale = TEST_LOCALE, assets = assets
        )

        val expectedUser = User(
            id = TEST_USER_ID,
            name = TEST_USER_NAME,
            email = TEST_EMAIL,
            profilePicture = userMapper.generateProfilePicture(assetMapper, assets)
        )

        userMapper.fromSelfUserResponse(selfUserResponse) shouldBeEqualTo expectedUser
        expectedUser.profilePicture.shouldBeInstanceOf<Asset>()
    }

    @Test
    fun `given generateProfilePicture is called, when asset key is valid, then returns a profile picture`() {
        val assetKey = "asset-key"
        val assetResponse = mockk<AssetResponse>()
        val assets = listOf(assetResponse)
        every { assetMapper.profilePictureAssetKey(assets) } returns assetKey

        val expectedProfilePicture = userMapper.generateProfilePicture(assetMapper, assets)

        expectedProfilePicture.shouldBeInstanceOf<Asset>()
    }

    @Test
    fun `given generateProfilePicture is called, when asset key is null, then returns profile picture should be null`() {
        every { assetMapper.profilePictureAssetKey(listOf()) } returns null

        val expectedProfilePicture = userMapper.generateProfilePicture(assetMapper, listOf())

        expectedProfilePicture.shouldBeNull()
    }

    companion object {
        private const val TEST_USER_ID = "test-id-123"
        private const val TEST_USER_NAME = "test-name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_LOCALE = "en-GB"
    }
}
