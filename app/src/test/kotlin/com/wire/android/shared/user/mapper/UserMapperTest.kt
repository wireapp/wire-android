package com.wire.android.shared.user.mapper

import com.wire.android.UnitTest
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.mapper.AssetMapper
import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.remote.SelfUserResponse
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBeEqualTo
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
        val profilePicture = PublicAsset("")
        val selfUserResponse = SelfUserResponse(
            id = TEST_USER_ID, name = TEST_USER_NAME,
            email = TEST_EMAIL, locale = TEST_LOCALE, assets = emptyList()
        )
        val expectedUser = User(
            id = TEST_USER_ID,
            name = TEST_USER_NAME,
            email = TEST_EMAIL,
            profilePicture = profilePicture
        )

        userMapper.fromSelfUserResponse(selfUserResponse) shouldBeEqualTo expectedUser
    }

    companion object {
        private const val TEST_USER_ID = "test-id-123"
        private const val TEST_USER_NAME = "test-name"
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_LOCALE = "en-GB"
    }
}
