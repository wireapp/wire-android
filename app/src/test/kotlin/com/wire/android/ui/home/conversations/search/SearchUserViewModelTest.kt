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

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.publicuser.model.UserSearchDetails
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class SearchUserViewModelTest {

    @Test
    fun `given addMembersSearchNavArgs are null, when calling the searchUseCase, then excludingMembersOfConversation is null`() = runTest {
        val query = "query"

        val (arrangement, viewModel) = Arrangement()
            .withAddMembersSearchNavArgsThatThrowsException()
            .withSearchResult(
                SearchUsersUseCase.Result(
                    connected = listOf(),
                    notConnected = listOf()
                )
            )
            .withFederatedSearchParserResult(
                FederatedSearchParser.Result(
                    searchTerm = query,
                    domain = "domain"
                )
            )
            .arrange()

        viewModel.safeSearch(query)
        coVerify(exactly = 1) {
            arrangement.searchUsersUseCase(
                query,
                excludingMembersOfConversation = null,
                customDomain = "domain"
            )
        }

        coVerify(exactly = 1) {
            arrangement.federatedSearchParser(any())
        }
    }

    @Test
    fun `given addMembersSearchNavArgs are not null, when calling the searchUseCase, then excludingMembersOfConversation is not null`() =
        runTest {
            val query = "query"

            val conversationId = ConversationId("id", "domain")
            val (arrangement, viewModel) = Arrangement()
                .withAddMembersSearchNavArgs(AddMembersSearchNavArgs(conversationId, true))
                .withSearchResult(
                    SearchUsersUseCase.Result(
                        connected = listOf(),
                        notConnected = listOf()
                    )
                )
                .withFederatedSearchParserResult(
                    FederatedSearchParser.Result(
                        searchTerm = query,
                        domain = "domain"
                    )
                )
                .arrange()

            viewModel.safeSearch(query)

            coVerify(exactly = 1) {
                arrangement.searchUsersUseCase(
                    query,
                    excludingMembersOfConversation = conversationId,
                    customDomain = "domain"
                )
            }

            coVerify(exactly = 1) {
                arrangement.federatedSearchParser(any())
            }
        }

    @Test
    fun `given searchUserUseCase returns a list of connected users, when calling the searchUseCase, then contactsResult is set`() =
        runTest {

            val result = SearchUsersUseCase.Result(
                connected = listOf(
                    UserSearchDetails(
                        id = UserId("connected", "domain"),
                        name = "connected",
                        completeAssetId = null,
                        type = UserType.INTERNAL,
                        connectionStatus = ConnectionState.ACCEPTED,
                        previewAssetId = null,
                        handle = "handle"
                    )
                ),
                notConnected = listOf(
                    UserSearchDetails(
                        id = UserId("notconnected", "domain"),
                        name = "notconnected",
                        completeAssetId = null,
                        type = UserType.INTERNAL,
                        connectionStatus = ConnectionState.BLOCKED,
                        previewAssetId = null,
                        handle = "handle"
                    )
                )
            )
            val query = "query"

            val (arrangement, viewModel) = Arrangement()
                .withAddMembersSearchNavArgsThatThrowsException()
                .withSearchResult(result)
                .withFederatedSearchParserResult(
                    FederatedSearchParser.Result(
                        searchTerm = query,
                        domain = "domain"
                    )
                )
                .arrange()

            viewModel.safeSearch(query)

            coVerify(exactly = 1) {
                arrangement.searchUsersUseCase(
                    query,
                    excludingMembersOfConversation = null,
                    customDomain = "domain"
                )
            }

            coVerify(exactly = 1) {
                arrangement.federatedSearchParser(any())
            }

            assertEquals(result.connected.map(arrangement::fromSearchUserResult), viewModel.state.contactsResult)
            assertEquals(result.notConnected.map(arrangement::fromSearchUserResult), viewModel.state.publicResult)
        }

    private class Arrangement {

        @MockK
        lateinit var searchUsersUseCase: SearchUsersUseCase

        @MockK
        lateinit var contactMapper: ContactMapper

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var federatedSearchParser: FederatedSearchParser

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { contactMapper.fromSearchUserResult(any()) } answers {
                val user = args.get(0) as UserSearchDetails
                fromSearchUserResult(user)
            }
        }

        fun fromSearchUserResult(user: UserSearchDetails): Contact {
            with(user) {
                return Contact(
                    id = id.value,
                    domain = id.domain,
                    name = name ?: String.EMPTY,
                    label = user.handle ?: String.EMPTY,
                    avatarData = UserAvatarData(asset = null),
                    membership = when (user.type) {
                        UserType.GUEST -> Membership.Guest
                        UserType.FEDERATED -> Membership.Federated
                        UserType.EXTERNAL -> Membership.External
                        UserType.INTERNAL -> Membership.Standard
                        UserType.NONE -> Membership.None
                        UserType.SERVICE -> Membership.Service
                        UserType.ADMIN -> Membership.Admin
                        UserType.OWNER -> Membership.Owner
                    },
                    connectionState = connectionStatus
                )
            }
        }

        fun withAddMembersSearchNavArgs(navArgs: AddMembersSearchNavArgs) = apply {
            every { savedStateHandle.navArgs<AddMembersSearchNavArgs>() } returns navArgs
        }

        fun withAddMembersSearchNavArgsThatThrowsException() = apply {
            every { savedStateHandle.navArgs<AddMembersSearchNavArgs>() } answers {
                throw RuntimeException()
            }
        }

        fun withSearchResult(result: SearchUsersUseCase.Result) = apply {
            coEvery { searchUsersUseCase(any(), any(), any()) } returns result
        }

        fun withFederatedSearchParserResult(result: FederatedSearchParser.Result) = apply {
            coEvery { federatedSearchParser(any()) } returns result
        }

        private lateinit var searchUserViewModel: SearchUserViewModel

        fun arrange() = apply {
            searchUserViewModel = SearchUserViewModel(
                searchUsersUseCase,
                contactMapper,
                federatedSearchParser,
                savedStateHandle
            )
        }.run {
            this to searchUserViewModel
        }
    }
}
