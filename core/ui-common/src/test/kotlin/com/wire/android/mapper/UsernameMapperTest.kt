package com.wire.android.mapper

import com.wire.android.mapper.UsernameMapper.fromOtherUser
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class UsernameMapperTest {

    @Test
    fun `given a guest temporary user, should map the handle as hours left`() {
        val expected = "22h"
        val result = fromOtherUser(
            OTHER_USER.copy(
                userType = UserTypeInfo.Regular(UserType.GUEST),
                expiresAt = Clock.System.now().plus(22.hours)
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `given a guest temporary user, and time left is less than 1 hour, should map the handle as minutes left`() {
        val expected = "10m"
        val result = fromOtherUser(
            OTHER_USER.copy(
                userType = UserTypeInfo.Regular(UserType.GUEST),
                expiresAt = Clock.System.now().plus(10.minutes)
            )
        )
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @EnumSource(TestParams::class)
    fun `should map other user to its username - handle accordingly`(params: TestParams) {
        assertEquals(params.expected, fromOtherUser(params.input), "Failed for input: <${params.input}>")
    }

    companion object {

        enum class TestParams(val input: OtherUser, val expected: String) {
            FEDERATED_USER(OTHER_USER.copy(userType = UserTypeInfo.Regular(UserType.FEDERATED), handle = "handle"), "handle@domain"),
            REGULAR_USER(OTHER_USER.copy(userType = UserTypeInfo.Regular(UserType.GUEST), handle = "handle"), "handle"),
            NO_HANDLE_USER(OTHER_USER.copy(userType = UserTypeInfo.Regular(UserType.INTERNAL), handle = null), "")
        }
    }
}

private val OTHER_USER = OtherUser(
    id = UserId("otherValue", "domain"),
    name = "otherUsername",
    handle = "otherHandle",
    email = "otherEmail",
    phone = "otherPhone",
    accentId = 0,
    teamId = TeamId("otherTeamId"),
    connectionStatus = ConnectionState.ACCEPTED,
    previewPicture = UserAssetId("value", "domain"),
    completePicture = UserAssetId("value", "domain"),
    availabilityStatus = UserAvailabilityStatus.AVAILABLE,
    userType = UserTypeInfo.Regular(UserType.INTERNAL),
    botService = null,
    deleted = false,
    defederated = false,
    isProteusVerified = false,
    supportedProtocols = setOf(SupportedProtocol.PROTEUS)
)
