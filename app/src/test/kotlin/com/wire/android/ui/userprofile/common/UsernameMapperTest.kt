package com.wire.android.ui.userprofile.common

import com.wire.android.framework.TestUser
import com.wire.android.ui.userprofile.common.UsernameMapper.fromOtherUser
import com.wire.kalium.logic.data.user.type.UserType
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class UsernameMapperTest {

    @Test
    fun `given a federated user, should map its handle with domain`() {
        val expected = "otherHandle@domain"
        val result = fromOtherUser(TestUser.OTHER_USER.copy(userType = UserType.FEDERATED))
        assertEquals(expected, result)
    }

    @Test
    fun `given a guest temporary user, should map the handle as hours left`() {
        val expected = "22h"
        val result = fromOtherUser(TestUser.OTHER_USER.copy(userType = UserType.GUEST, expiresAt = Clock.System.now().plus(22.hours)))
        assertEquals(expected, result)
    }

    @Test
    fun `given some user, non federated or guest, should just map the handle`() {
        val expected = "otherHandle"
        val result = fromOtherUser(TestUser.OTHER_USER.copy(userType = UserType.INTERNAL))
        assertEquals(expected, result)
    }

}
