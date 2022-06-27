package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.home.conversations.handle
import com.wire.android.ui.home.conversations.model.UIParticipant
import com.wire.android.ui.home.conversations.name
import com.wire.android.ui.home.conversations.userId
import com.wire.android.ui.home.conversations.userType
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import io.mockk.MockKAnnotations
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UIParticipantMapperTest {

    @Test
    fun givenMemberDetails_whenMappingToContacts_thenCorrectValuesShouldBeReturned() = runTest {
        val (arrangement, mapper) = Arrangement().arrange()
        // Given
        val data = listOf(
            MemberDetails.Self(testSelfUser(0)),
            MemberDetails.Other(testOtherUser(1).copy(userType = UserType.INTERNAL)),
            MemberDetails.Other(testOtherUser(2).copy(userType = UserType.EXTERNAL)),
            MemberDetails.Other(testOtherUser(3).copy(userType = UserType.FEDERATED)),
            MemberDetails.Other(testOtherUser(4).copy(userType = UserType.GUEST))
        )
        // When
        val results = data.map { mapper.toUIParticipant(it) }
        // Then
        results.forEachIndexed { index, result ->
            assert(compareResult(data[index], result, arrangement.userTypeMapper))
        }
    }

    private fun compareResult(memberDetails: MemberDetails, uiParticipant: UIParticipant, userTypeMapper: UserTypeMapper) =
        memberDetails.userId == uiParticipant.id
                && memberDetails.name == uiParticipant.name
                && memberDetails.handle == uiParticipant.handle
                && memberDetails.avatar == uiParticipant.avatarData
                && userTypeMapper.toMembership(memberDetails.userType) == uiParticipant.membership
                && memberDetails is MemberDetails.Self == uiParticipant.isSelf

    private class Arrangement {

        val userTypeMapper: UserTypeMapper = UserTypeMapper()
        private val mapper: UIParticipantMapper = UIParticipantMapper(userTypeMapper)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun arrange() = this to mapper
    }
}

fun testSelfUser(i: Int): SelfUser = SelfUser(
    id = UserId(value = "value$i", domain = "domain$i"),
    name = "name$i",
    handle = "handle$i",
    email = "email$i",
    phone = "phone$i",
    accentId = i,
    teamId = "team$i",
    connectionStatus = ConnectionState.NOT_CONNECTED,
    previewPicture = null,
    completePicture = null,
    availabilityStatus = UserAvailabilityStatus.NONE
)

fun testOtherUser(i: Int): OtherUser = OtherUser(
    id = UserId(value = "value$i", domain = "domain$i"),
    name = "name$i",
    handle = "handle$i",
    email = "email$i",
    phone = "phone$i",
    accentId = i,
    team = "team$i",
    connectionStatus = ConnectionState.NOT_CONNECTED,
    previewPicture = null,
    completePicture = null,
    availabilityStatus = UserAvailabilityStatus.NONE,
    userType = UserType.INTERNAL
)
