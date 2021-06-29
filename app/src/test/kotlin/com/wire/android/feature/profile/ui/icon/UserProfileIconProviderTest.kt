package com.wire.android.feature.profile.ui.icon

import com.wire.android.UnitTest
import com.wire.android.shared.asset.ui.imageloader.AvatarLoader
import com.wire.android.shared.user.User
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class UserProfileIconProviderTest : UnitTest() {

    @MockK
    private lateinit var avatarLoader: AvatarLoader

    private lateinit var userProfileIconProvider: UserProfileIconProvider

    @Before
    fun setUp() {
        userProfileIconProvider = UserProfileIconProvider(avatarLoader)
    }

    @Test
    fun `given provide() is called, when user is valid, then return UserProfileIcon`() {
        val user = mockk<User>()

        val icon = userProfileIconProvider.provide(user)

        icon shouldBeInstanceOf UserProfileIcon::class
    }
}
