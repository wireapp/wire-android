/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversations.details.participants.usecase

import com.wire.android.framework.TestConversation
import com.wire.android.mapper.testOtherUser
import com.wire.android.mapper.testSelfUser
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group.Channel.ChannelAccess
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group.Channel.ChannelAddPermission
import com.wire.kalium.logic.data.conversation.ConversationHistorySettings
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObserveConversationRoleForUserUseCaseTest {

    @Test
    fun `ordinary conversation projects member roles`() = runTest {
        val self = testSelfUser(0)
        val other = testOtherUser(1)
        val conversationDetails = ConversationDetails.Group.Regular(
            conversation = TestConversation.GROUP(),
            isSelfUserMember = true,
            selfRole = Conversation.Member.Role.Admin,
        )
        val useCase = useCase(
            self = self,
            details = ObserveConversationDetailsUseCase.Result.Success(conversationDetails),
            members = listOf(
                MemberDetails(self, Conversation.Member.Role.Admin),
                MemberDetails(other, Conversation.Member.Role.Member),
            ),
        )

        val result = useCase(TestConversation.ID, other.id).first()

        assertEquals(TestConversation.GROUP().name, result.conversationName)
        assertEquals(Conversation.Member.Role.Member, result.userRole)
        assertEquals(Conversation.Member.Role.Admin, result.selfRole)
        assertEquals(TestConversation.ID, result.conversationId)
    }

    @Test
    fun `same-team channel overrides team-admin self role`() = runTest {
        val self = teamAdmin()
        val other = testOtherUser(1)
        val useCase = useCase(
            self = self,
            details = ObserveConversationDetailsUseCase.Result.Success(
                channelDetails(teamId = requireNotNull(self.teamId)),
            ),
            members = listOf(
                MemberDetails(self, Conversation.Member.Role.Member),
                MemberDetails(other, Conversation.Member.Role.Member),
            ),
        )

        val result = useCase(TestConversation.ID, other.id).first()

        assertEquals(Conversation.Member.Role.Admin, result.selfRole)
    }

    @Test
    fun `cross-team channel keeps member-derived self role`() = runTest {
        val self = teamAdmin()
        val other = testOtherUser(1)
        val useCase = useCase(
            self = self,
            details = ObserveConversationDetailsUseCase.Result.Success(
                channelDetails(teamId = TeamId("another-team")),
            ),
            members = listOf(
                MemberDetails(self, Conversation.Member.Role.Member),
                MemberDetails(other, Conversation.Member.Role.Member),
            ),
        )

        val result = useCase(TestConversation.ID, other.id).first()

        assertEquals(Conversation.Member.Role.Member, result.selfRole)
    }

    @Test
    fun `failed conversation details do not emit a role projection`() = runTest {
        val self = testSelfUser(0)
        val useCase = useCase(
            self = self,
            details = ObserveConversationDetailsUseCase.Result.Failure(StorageFailure.DataNotFound),
            members = listOf(MemberDetails(self, Conversation.Member.Role.Member)),
        )

        val results = useCase(TestConversation.ID, self.id).toList()

        assertTrue(results.isEmpty())
    }

    private fun useCase(
        self: SelfUser,
        details: ObserveConversationDetailsUseCase.Result,
        members: List<MemberDetails>,
    ): ObserveConversationRoleForUserUseCase {
        val observeConversationMembers = mockk<ObserveConversationMembersUseCase>()
        val observeConversationDetails = mockk<ObserveConversationDetailsUseCase>()
        val observeSelfUser = mockk<ObserveSelfUserUseCase>()
        coEvery { observeConversationMembers(any()) } returns flowOf(members)
        coEvery { observeConversationDetails(any()) } returns flowOf(details)
        coEvery { observeSelfUser() } returns flowOf(self)
        return ObserveConversationRoleForUserUseCase(
            observeConversationMembers,
            observeConversationDetails,
            observeSelfUser,
        )
    }

    private fun teamAdmin(): SelfUser = testSelfUser(0).copy(
        userType = UserTypeInfo.Regular(UserType.ADMIN),
    )

    private fun channelDetails(teamId: TeamId): ConversationDetails.Group.Channel =
        ConversationDetails.Group.Channel(
            conversation = TestConversation.GROUP().copy(
                type = Conversation.Type.Group.Channel,
                teamId = teamId,
            ),
            isSelfUserMember = true,
            selfRole = Conversation.Member.Role.Member,
            access = ChannelAccess.PRIVATE,
            permission = ChannelAddPermission.ADMINS,
            historySharing = ConversationHistorySettings.Private,
        )
}
