/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.handle
import com.wire.android.ui.home.conversations.name
import com.wire.android.ui.home.conversations.userId
import com.wire.android.ui.home.conversations.userType
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UIParticipantMapperTest {

    @Test
    fun givenMemberDetails_whenMappingToContacts_thenCorrectValuesShouldBeReturned() = runTest {
        val (arrangement, mapper) = Arrangement().arrange()
        // Given
        val data: List<MemberDetails> = listOf(
            MemberDetails(testSelfUser(0), Member.Role.Admin),
            MemberDetails(testOtherUser(1).copy(userType = UserType.INTERNAL), Member.Role.Admin),
            MemberDetails(testOtherUser(2).copy(userType = UserType.EXTERNAL), Member.Role.Member),
            MemberDetails(testOtherUser(3).copy(userType = UserType.FEDERATED), Member.Role.Member),
            MemberDetails(testOtherUser(4).copy(userType = UserType.GUEST), Member.Role.Member)
        )
        // When
        val results = data.map { mapper.toUIParticipant(it.user) }
        // Then
        results.forEachIndexed { index, result ->
            assert(compareResult(arrangement.wireSessionImageLoader, data[index], result, arrangement.userTypeMapper))
        }
    }

    private fun compareResult(
        wireSessionImageLoader: WireSessionImageLoader,
        memberDetails: MemberDetails,
        uiParticipant: UIParticipant,
        userTypeMapper: UserTypeMapper
    ): Boolean {
        val connectionState = if (uiParticipant.isSelf) null else ConnectionState.NOT_CONNECTED
        return (memberDetails.userId == uiParticipant.id
                && memberDetails.name == uiParticipant.name
                && memberDetails.handle == uiParticipant.handle
                && memberDetails.user.avatar(wireSessionImageLoader, connectionState) == uiParticipant.avatarData
                && userTypeMapper.toMembership(memberDetails.userType) == uiParticipant.membership
                && memberDetails.user is SelfUser == uiParticipant.isSelf)
    }

    private class Arrangement {

        @MockK
        lateinit var wireSessionImageLoader: WireSessionImageLoader

        val userTypeMapper: UserTypeMapper = UserTypeMapper()

        private val mapper: UIParticipantMapper by lazy {
            UIParticipantMapper(userTypeMapper, wireSessionImageLoader)
        }

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
    teamId = TeamId("team$i"),
    connectionStatus = ConnectionState.NOT_CONNECTED,
    previewPicture = null,
    completePicture = null,
    availabilityStatus = UserAvailabilityStatus.NONE,
    supportedProtocols = setOf(SupportedProtocol.PROTEUS)
)

fun testOtherUser(i: Int): OtherUser = OtherUser(
    id = UserId(value = "value$i", domain = "domain$i"),
    name = "name$i",
    handle = "handle$i",
    email = "email$i",
    phone = "phone$i",
    accentId = i,
    teamId = TeamId("team$i"),
    connectionStatus = ConnectionState.NOT_CONNECTED,
    previewPicture = null,
    completePicture = null,
    availabilityStatus = UserAvailabilityStatus.NONE,
    userType = UserType.INTERNAL,
    botService = null,
    deleted = false,
    defederated = false,
    supportedProtocols = setOf(SupportedProtocol.PROTEUS)
)

fun testUIParticipant(i: Int): UIParticipant = UIParticipant(
    id = UserId(value = "value$i", domain = "domain$i"),
    name = "name$i",
    handle = "handle$i",
    isSelf = false
)
