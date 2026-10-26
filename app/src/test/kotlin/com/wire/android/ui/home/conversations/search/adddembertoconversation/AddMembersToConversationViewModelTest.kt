/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.search.adddembertoconversation

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.model.Contact
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AddMembersToConversationViewModelTest {

    @Test
    fun `disabled feature allows pending snapshots to be selected`() = runTest {
        val (_, viewModel) = Arrangement().arrange {
            pendingConnectionsEnabled = false
            withAddMembersSearchNavArgs(
                AddMembersSearchNavArgs(
                    conversationId = ConversationId("conversation", "domain"),
                    isConversationAppsEnabled = false,
                    isSelfPartOfATeam = true,
                    protocolInfo = Conversation.ProtocolInfo.Proteus
                )
            )
        }
        val pending = Contact(
            "pending",
            "domain",
            "Pending",
            "pending",
            connectionState = ConnectionState.ACCEPTED,
            membership = Membership.Standard,
            contactStatus = com.wire.kalium.logic.data.user.ContactStatus.NON_CONTACTABLE
        )
        viewModel.updateContactStatuses(mapOf(com.wire.kalium.logic.data.user.UserId(pending.id, pending.domain) to pending.contactStatus))
        viewModel.updateSelectedContacts(true, pending)
        assertEquals(false, viewModel.newGroupState.selectedContacts.single().isOnboardingPending)
        viewModel.updateSelectedContacts(false, pending)
        assertEquals(true, viewModel.newGroupState.selectedContacts.isEmpty())
    }

    @Test
    fun `pending users cannot be selected but can be deselected`() {
        val (_, viewModel) = Arrangement().arrange {
            withAddMembersSearchNavArgs(
                AddMembersSearchNavArgs(
                    conversationId = ConversationId("conversation", "domain"),
                    isConversationAppsEnabled = false,
                    isSelfPartOfATeam = true,
                    protocolInfo = Conversation.ProtocolInfo.Proteus
                )
            )
        }
        val pending = Contact(
            "pending",
            "domain",
            "Pending",
            "pending",
            connectionState = ConnectionState.ACCEPTED,
            membership = Membership.Standard,
            contactStatus = com.wire.kalium.logic.data.user.ContactStatus.NON_CONTACTABLE
        )
        viewModel.updateSelectedContacts(true, pending)
        assertEquals(persistentSetOf<Contact>(), viewModel.newGroupState.selectedContacts)
        viewModel.updateSelectedContacts(true, pending.copy(contactStatus = null))
        val userId = com.wire.kalium.logic.data.user.UserId(pending.id, pending.domain)
        viewModel.updateContactStatuses(mapOf(userId to pending.contactStatus))
        assertEquals(true, viewModel.newGroupState.selectedContacts.single().isOnboardingPending)
        viewModel.updateSelectedContacts(false, pending)
        viewModel.updateSelectedContacts(true, pending.copy(contactStatus = null))
        assertEquals(true, viewModel.newGroupState.selectedContacts.isEmpty())
        viewModel.updateContactStatuses(mapOf(userId to null))
        viewModel.updateSelectedContacts(true, pending)
        assertEquals(false, viewModel.newGroupState.selectedContacts.single().isOnboardingPending)
        viewModel.updateSelectedContacts(false, pending)
        assertEquals(persistentSetOf<Contact>(), viewModel.newGroupState.selectedContacts)
    }

    @Test
    fun `given content, when adding to selected contacts, then it is added`() {
        val (_, viewModel) = Arrangement()
            .arrange {
                withAddMembersSearchNavArgs(
                    AddMembersSearchNavArgs(
                        conversationId = ConversationId("conversationId", "domain"),
                        isConversationAppsEnabled = false,
                        isSelfPartOfATeam = true,
                        protocolInfo = Conversation.ProtocolInfo.Proteus
                    )
                )
            }

        val expected = Contact(
            "id",
            "domain",
            "name",
            "handle",
            UserAvatarData(),
            label = "label",
            connectionState = ConnectionState.ACCEPTED,
            membership = Membership.External
        )

        viewModel.updateSelectedContacts(true, expected)

        assertEquals(persistentSetOf(expected), viewModel.newGroupState.selectedContacts)
    }

    @Test
    fun `given content, when removing from selected contacts, then it is removed`() {
        val (_, viewModel) = Arrangement()
            .arrange {
                withAddMembersSearchNavArgs(
                    AddMembersSearchNavArgs(
                        conversationId = ConversationId("conversationId", "domain"),
                        isConversationAppsEnabled = false,
                        isSelfPartOfATeam = true,
                        protocolInfo = Conversation.ProtocolInfo.Proteus
                    )
                )
            }

        val expected = Contact(
            "id",
            "domain",
            "name",
            "handle",
            UserAvatarData(),
            label = "label",
            connectionState = ConnectionState.ACCEPTED,
            membership = Membership.External
        )

        viewModel.updateSelectedContacts(true, expected)
        assertEquals(persistentSetOf(expected), viewModel.newGroupState.selectedContacts)

        viewModel.updateSelectedContacts(false, expected)
        assertEquals(persistentSetOf<Contact>(), viewModel.newGroupState.selectedContacts)
    }

    @Test
    fun `given contact, when adding to group conversation, then use case is called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange {
                withAddMembersSearchNavArgs(
                    AddMembersSearchNavArgs(
                        conversationId = ConversationId("conversationId", "domain"),
                        isConversationAppsEnabled = false,
                        isSelfPartOfATeam = true,
                        protocolInfo = Conversation.ProtocolInfo.Proteus
                    )
                )
                withAddMemberToConversationUseCase(AddMemberToConversationUseCase.Result.Success)
            }

        val expected = Contact(
            "id",
            "domain",
            "name",
            "handle",
            UserAvatarData(),
            label = "label",
            connectionState = ConnectionState.ACCEPTED,
            membership = Membership.External
        )

        viewModel.updateSelectedContacts(true, expected)

        viewModel.addMembersToConversation()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.addMemberToConversationUseCase(
                conversationId = ConversationId("conversationId", "domain"),
                userIdList = listOf(UserId(expected.id, expected.domain))
            )
        }
        assertEquals(true, viewModel.newGroupState.isCompleted)
    }

    private class Arrangement {

        @MockK
        lateinit var addMemberToConversationUseCase: AddMemberToConversationUseCase

        val testDispatchers = TestDispatcherProvider()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        var pendingConnectionsEnabled = true

        lateinit var viewModel: AddMembersToConversationViewModel

        private lateinit var navArgs: AddMembersSearchNavArgs

        fun withAddMembersSearchNavArgs(navArgs: AddMembersSearchNavArgs) {
            this.navArgs = navArgs
        }

        fun withAddMemberToConversationUseCase(result: AddMemberToConversationUseCase.Result) {
            coEvery { addMemberToConversationUseCase(any(), any()) } returns result
        }

        fun arrange(block: Arrangement.() -> Unit): Pair<Arrangement, AddMembersToConversationViewModel> = apply(block).let {
            viewModel = AddMembersToConversationViewModel(
                addMemberToConversation = addMemberToConversationUseCase,
                dispatchers = testDispatchers,
                addMembersSearchNavArgs = navArgs,
                kaliumConfigs = KaliumConfigs(pendingConnections = pendingConnectionsEnabled),
            )
            this to viewModel
        }
    }
}
