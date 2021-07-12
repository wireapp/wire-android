package com.wire.android.feature.profile.ui.icon

import com.wire.android.UnitTest
import com.wire.android.shared.asset.Asset
import com.wire.android.shared.asset.ui.imageloader.AvatarLoader
import com.wire.android.shared.asset.ui.imageloader.UserAvatar
import com.wire.android.shared.asset.ui.imageloader.UserAvatarProvider
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class UserAvatarProviderTest : UnitTest() {

    @MockK
    private lateinit var avatarLoader: AvatarLoader

    private lateinit var userAvatarProvider: UserAvatarProvider

    @Before
    fun setUp() {
        userAvatarProvider = UserAvatarProvider(avatarLoader)
    }

    @Test
    fun `given provide() is called, when passed data are valid, then return UserAvatar`() {
        val name = "Someone"
        val profilePicture = mockk<Asset>()

        val icon = userAvatarProvider.provide(profilePicture, name)

        icon shouldBeInstanceOf UserAvatar::class
    }
}
