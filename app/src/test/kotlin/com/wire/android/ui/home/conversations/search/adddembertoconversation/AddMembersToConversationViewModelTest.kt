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

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class AddMembersToConversationViewModelTest {

    @Test
    fun `given content, when adding to selected contacts, then it is added`() {
        val (_, viewModel) = Arrangement()
            .arrange {
                withAddMembersSearchNavArgs(
                    AddMembersSearchNavArgs(
                        conversationId = ConversationId("conversationId", "domain"),
                        isServicesAllowed = false
                    )
                )
            }

        val expected = Contact(
            "id",
            "domain",
            "name",
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
                        isServicesAllowed = false
                    )
                )
            }

        val expected = Contact(
            "id",
            "domain",
            "name",
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
    fun `given contact, when adding to group conversion, then use case is called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange {
                withAddMembersSearchNavArgs(
                    AddMembersSearchNavArgs(
                        conversationId = ConversationId("conversationId", "domain"),
                        isServicesAllowed = false
                    )
                )
            }

        val expected = Contact(
            "id",
            "domain",
            "name",
            UserAvatarData(),
            label = "label",
            connectionState = ConnectionState.ACCEPTED,
            membership = Membership.External
        )

        viewModel.updateSelectedContacts(true, expected)

        viewModel.addMembersToConversation(onCompleted = {})
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.addMemberToConversionUseCase(
                conversationId = ConversationId("conversationId", "domain"),
                userIdList = listOf(UserId(expected.id, expected.domain))
            )
        }
    }

    private class Arrangement {

        @MockK
        lateinit var addMemberToConversionUseCase: AddMemberToConversationUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        val testDispatchers = TestDispatcherProvider()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        lateinit var viewModel: AddMembersToConversationViewModel

        fun withAddMembersSearchNavArgs(navArgs: AddMembersSearchNavArgs) {
            every { savedStateHandle.navArgs<AddMembersSearchNavArgs>() } returns navArgs
        }

        fun arrange(block: Arrangement.() -> Unit): Pair<Arrangement, AddMembersToConversationViewModel> = apply(block).let {
            viewModel = AddMembersToConversationViewModel(
                addMemberToConversation = addMemberToConversionUseCase,
                dispatchers = testDispatchers,
                savedStateHandle = savedStateHandle
            )
            this to viewModel
        }
    }
}
