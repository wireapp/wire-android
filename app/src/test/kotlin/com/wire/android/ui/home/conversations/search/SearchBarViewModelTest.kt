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
package com.wire.android.ui.home.conversations.search

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.amshove.kluent.internal.assertFails
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class SearchBarViewModelTest {

    @Test
    fun `given isServiceAllowed false and a conversationId, then state is updated correctly`() {
        val (_, viewModel) = Arrangement()
            .withAddMembersSearchNavArgs(
                AddMembersSearchNavArgs(
                    conversationId = ConversationId("conversationId", "domain"),
                    isServicesAllowed = false
                )
            )
            .arrange()

        assertFalse(viewModel.state.isServicesAllowed)
    }

    @Test
    fun `given isServiceAllowed true and a conversationId, then state is updated correctly`() {
        val (_, viewModel) = Arrangement()
            .withAddMembersSearchNavArgs(
                AddMembersSearchNavArgs(
                    conversationId = ConversationId("conversationId", "domain"),
                    isServicesAllowed = true
                )
            )
            .arrange()

        assertTrue(viewModel.state.isServicesAllowed)
    }

    @Test
    fun `given a new user search query, when updating, then there is a delay to the search signal`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        viewModel.userSearchSignal.test {
            viewModel.onUserSearchQueryChanged(TextFieldValue("query"))
            assertFails {
                withTimeout(450) {
                    awaitItem()
                }
            }
            awaitItem().also {
                assertTrue(it == "query")
            }
        }
    }

    @Test
    fun `given a new service search query, when updating, then there is a delay to the search signal`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        viewModel.serviceSearchSignal.test {
            viewModel.onServiceSearchQueryChanged(TextFieldValue("query"))
            assertFails {
                withTimeout(450) {
                    awaitItem()
                }
            }
            awaitItem().also {
                assertTrue(it == "query")
            }
        }
    }

    private class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withAddMembersSearchNavArgs(navArgs: AddMembersSearchNavArgs) = apply {
            every { savedStateHandle.navArgs<AddMembersSearchNavArgs>() } returns navArgs
        }

        fun withAddMembersSearchNavArgsThatThrowsException() = apply {
            every { savedStateHandle.navArgs<AddMembersSearchNavArgs>() } answers {
                throw RuntimeException()
            }
        }

        private lateinit var searchBarViewModel: SearchBarViewModel

        fun arrange() = apply {
            searchBarViewModel = SearchBarViewModel(savedStateHandle)
        }.run {
            this to searchBarViewModel
        }
    }
}
