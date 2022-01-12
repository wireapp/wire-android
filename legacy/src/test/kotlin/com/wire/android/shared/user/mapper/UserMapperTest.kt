package com.wire.android.shared.user.mapper

import com.wire.android.UnitTest
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import com.wire.android.shared.asset.mapper.AssetMapper
import com.wire.android.shared.user.datasources.remote.SelfUserResponse
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
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
    fun `given fromSelfUserResponse is called when userResponse has a profile picture, then return a user with profile picture`() {
        val assetKey = "asset_key"
        val assetResponse = mockk<AssetResponse>()
        val assets = listOf(assetResponse)
        val selfUserResponse = SelfUserResponse(
            id = TEST_USER_ID, name = TEST_USER_NAME,
            email = TEST_EMAIL, locale = TEST_LOCALE, assets = assets
        )
        every { assetMapper.profilePictureAssetKey(assets) } returns assetKey

        val user = userMapper.fromSelfUserResponse(selfUserResponse)

        user.id.shouldBeEqualTo(TEST_USER_ID)
        user.name.shouldBeEqualTo(TEST_USER_NAME)
        user.email.shouldBeEqualTo(TEST_EMAIL)
        user.profilePicture.shouldBeEqualTo(PublicAsset(assetKey))
    }


    @Test
    fun `given fromSelfUserResponse is called when user does not have a profile picture, then return a user with null picture`() {
        val assets = listOf<AssetResponse>()
        val selfUserResponse = SelfUserResponse(
            id = TEST_USER_ID, name = TEST_USER_NAME,
            email = TEST_EMAIL, locale = TEST_LOCALE, assets = assets
        )
        every { assetMapper.profilePictureAssetKey(assets) } returns null

        val user = userMapper.fromSelfUserResponse(selfUserResponse)

        user.id.shouldBeEqualTo(TEST_USER_ID)
        user.name.shouldBeEqualTo(TEST_USER_NAME)
        user.email.shouldBeEqualTo(TEST_EMAIL)
        user.profilePicture.shouldBeNull()
    }

    companion object {
        private const val TEST_USER_ID = "test-id-123"
        private const val TEST_USER_NAME = "test-name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_LOCALE = "en-GB"
    }
}
