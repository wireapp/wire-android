/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.userprofile.other

import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.Test

class OtherUserProfileStateTest {

    @Test
    fun givenATemporaryUser_thenShouldNotAllowRoleModification() = runTest {
        val state = provideState(withExpireAt = Instant.DISTANT_FUTURE.toEpochMilliseconds())
        assertEquals(false, state.isRoleEditable())
    }

    @Test
    fun givenAUserWithoutMetadata_thenShouldNotAllowRoleModification() = runTest {
        val state = provideState(withUserName = "", withFullName = "")
        assertEquals(false, state.isRoleEditable())
    }

    @Test
    fun givenAFederatedUser_thenShouldNotAllowRoleModification() = runTest {
        val state = provideState(withMembership = Membership.Federated)
        assertEquals(false, state.isRoleEditable())
    }

    @Test
    fun givenAService_thenShouldNotAllowRoleModification() = runTest {
        val state = provideState(withMembership = Membership.Service)
        assertEquals(false, state.isRoleEditable())
    }

    private val baseState = OtherUserProfileState(
        userId = UserId("some_user", "domain.com"),
        fullName = "name",
        userName = "username",
        teamName = "team",
        email = "email",
        groupState = OtherUserProfileGroupState(
            groupName = "group name",
            role = Member.Role.Member,
            isSelfAdmin = true,
            conversationId = ConversationId("some_user", "domain.com")
        )
    )

    private fun provideState(
        withFullName: String = "name",
        withUserName: String = "username",
        withExpireAt: Long? = null,
        withMembership: Membership = Membership.Standard
    ): OtherUserProfileState {
        return baseState.copy(
            fullName = withFullName,
            userName = withUserName,
            expiresAt = withExpireAt?.let { Instant.fromEpochMilliseconds(it) },
            membership = withMembership
        )
    }
}
