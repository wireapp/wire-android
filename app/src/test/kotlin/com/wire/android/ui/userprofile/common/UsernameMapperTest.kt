package com.wire.android.ui.userprofile.common

import com.wire.android.framework.TestUser.OTHER_USER
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.type.UserType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class UsernameMapperTest {

    @ParameterizedTest
    @EnumSource(TestParams::class)
    fun `should map other user to its username - handle accordingly`(params: TestParams) {
        assertEquals(params.expected, UsernameMapper.fromOtherUser(params.input), "Failed for input: <${params.input}>")
    }

    companion object {

        enum class TestParams(val input: OtherUser, val expected: String) {
            FEDERATED_USER(OTHER_USER.copy(userType = UserType.FEDERATED, handle = "handle"), "handle@domain"),
            REGULAR_USER(OTHER_USER.copy(userType = UserType.GUEST, handle = "handle"), "handle"),
            NO_HANDLE_USER(OTHER_USER.copy(userType = UserType.INTERNAL, handle = null), "")
        }
    }
}
