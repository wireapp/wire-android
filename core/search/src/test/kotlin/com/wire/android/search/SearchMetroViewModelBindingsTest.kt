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
package com.wire.android.search

import com.wire.android.search.apps.SearchAppsViewModel
import com.wire.android.search.users.SearchUserViewModel
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class SearchMetroViewModelBindingsTest {

    @Test
    fun givenRuntimeArguments_whenCreatingSearchUserViewModel_thenFocusedFactoryReceivesExactArguments() {
        val conversationId = ConversationId("conversation", "domain")
        val expectedViewModel = mockk<SearchUserViewModel>()
        val searchUserFactory = mockk<SearchUserViewModel.Factory> {
            every { create(conversationId, true) } returns expectedViewModel
        }
        val adapter = SearchMetroViewModelBindings.searchManualViewModelFactory(
            searchUserFactory = searchUserFactory,
            searchAppsFactory = mockk<SearchAppsViewModel.Factory>(),
        ) as SearchManualViewModelFactory

        val actualViewModel = adapter.searchUserViewModel(conversationId, true)

        assertSame(expectedViewModel, actualViewModel)
        verify(exactly = 1) { searchUserFactory.create(conversationId, true) }
    }

    @Test
    fun givenNullableProtocolInfo_whenCreatingSearchAppsViewModel_thenFocusedFactoryReceivesExactArguments() {
        val protocolInfo = Conversation.ProtocolInfo.Proteus
        val expectedViewModelWithoutProtocol = mockk<SearchAppsViewModel>()
        val expectedViewModelWithProtocol = mockk<SearchAppsViewModel>()
        val searchAppsFactory = mockk<SearchAppsViewModel.Factory> {
            every { create(null) } returns expectedViewModelWithoutProtocol
            every { create(protocolInfo) } returns expectedViewModelWithProtocol
        }
        val adapter = SearchMetroViewModelBindings.searchManualViewModelFactory(
            searchUserFactory = mockk<SearchUserViewModel.Factory>(),
            searchAppsFactory = searchAppsFactory,
        ) as SearchManualViewModelFactory

        val actualViewModelWithoutProtocol = adapter.searchAppsViewModel(null)
        val actualViewModelWithProtocol = adapter.searchAppsViewModel(protocolInfo)

        assertSame(expectedViewModelWithoutProtocol, actualViewModelWithoutProtocol)
        assertSame(expectedViewModelWithProtocol, actualViewModelWithProtocol)
        verify(exactly = 1) { searchAppsFactory.create(null) }
        verify(exactly = 1) { searchAppsFactory.create(protocolInfo) }
    }
}
