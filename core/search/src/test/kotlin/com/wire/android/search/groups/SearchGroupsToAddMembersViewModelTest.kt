/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.wire.android.search.groups

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.Contact
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithMembers
import com.wire.kalium.logic.data.conversation.ConversationMembersQueryConfig
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithMembersBySearchQueryUseCase
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Suppress("UnusedFlow")
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class SearchGroupsToAddMembersViewModelTest {
    private val team = TeamId("team")

    @Test
    fun searchReturnsMatchingGroupsAndChannelsWithEligibleMembers() = runTest {
        val group = regularGroup("group", "Alpha group")
        val channel = channel("channel", "Alpha channel", ConversationDetails.Group.Channel.ChannelAccess.PRIVATE)
        val member = user("member", "wire.com", team)
        val contact = mockk<Contact>()
        val members = listOf(MemberDetails(member, Conversation.Member.Role.Member))
        val (arrangement, viewModel) = Arrangement()
            .withOnlySelfTeamAndDomain(true)
            .withSearchResults(
                listOf(
                    ConversationDetailsWithMembers(group, members),
                    ConversationDetailsWithMembers(channel, members),
                )
            )
            .withMappedContact(member, contact)
            .arrange()

        viewModel.search("Alpha")
        val groups = viewModel.groups.asSnapshot()

        assertEquals(setOf(group.conversation.id, channel.conversation.id), groups.map { it.id }.toSet())

        coVerify {
            val queryConfigMatch = match<ConversationMembersQueryConfig> { it.searchQuery == "Alpha" && it.onlyInteractionEnabled }
            arrangement.searchConversations(queryConfigMatch, any(), 0L, false, true)
        }
        assertEquals(setOf(contact), groups.first().members)
        assertEquals(false, groups.first { it.id == group.conversation.id }.isChannel)
        assertEquals(true, groups.first { it.id == channel.conversation.id }.isChannel)
        assertEquals(true, groups.first { it.id == channel.conversation.id }.isPrivateChannel)
    }

    @Test
    fun unrestrictedSearchAllowsExternalMembers() = runTest {
        val group = regularGroup("external", "External")
        val member = user("remote", "remote.com", null)
        val (arrangement, viewModel) = Arrangement()
            .withOnlySelfTeamAndDomain(false)
            .withSearchResults(
                listOf(ConversationDetailsWithMembers(group, listOf(MemberDetails(member, Conversation.Member.Role.Member))))
            )
            .withMappedContact(member, mockk())
            .arrange()

        val groups = viewModel.groups.asSnapshot()

        assertEquals(listOf(group.conversation.id), groups.map { it.id })
        coVerify {
            arrangement.searchConversations(any(), any(), 0L, false, false)
        }
    }

    private class Arrangement {
        @MockK
        lateinit var searchConversations: GetPaginatedFlowOfConversationDetailsWithMembersBySearchQueryUseCase

        @MockK
        lateinit var contactMapper: ContactMapper

        private var onlySelfTeamAndDomain: Boolean = false

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }
        fun withOnlySelfTeamAndDomain(onlySelfTeamAndDomain: Boolean) = apply {
            this.onlySelfTeamAndDomain = onlySelfTeamAndDomain
        }
        fun withSearchResults(conversations: List<ConversationDetailsWithMembers>) = apply {
            coEvery { searchConversations(any(), any(), 0L, false, any()) } returns flowOf(
                PagingData.from(
                    conversations,
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.NotLoading(true),
                        prepend = LoadState.NotLoading(true),
                        append = LoadState.NotLoading(true),
                    ),
                )
            )
        }
        fun withMappedContact(user: OtherUser, contact: Contact) = apply {
            every { contactMapper.fromOtherUser(user) } returns contact
        }
        fun arrange() = this to SearchGroupsToAddMembersViewModel(onlySelfTeamAndDomain, searchConversations, contactMapper)
    }

    private fun regularGroup(id: String, name: String): ConversationDetails.Group.Regular = mockk {
        every { conversation } returns conversation(id, name)
        every { isSelfUserMember } returns true
    }
    private fun channel(
        id: String,
        name: String,
        channelAccess: ConversationDetails.Group.Channel.ChannelAccess = ConversationDetails.Group.Channel.ChannelAccess.PUBLIC,
    ): ConversationDetails.Group.Channel = mockk {
        every { conversation } returns conversation(id, name)
        every { isSelfUserMember } returns true
        every { access } returns channelAccess
    }
    private fun conversation(id: String, name: String): Conversation = mockk<Conversation>().also { conversation ->
        every { conversation.id } returns ConversationId(id, "wire.com")
        every { conversation.name } returns name
    }
    private fun user(id: String, domain: String, teamId: TeamId?): OtherUser = mockk<OtherUser>().also { user ->
        every { user.id } returns UserId(id, domain)
        every { user.teamId } returns teamId
    }
}
