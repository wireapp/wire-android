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
import com.wire.android.config.SnapshotExtension
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.mls.CipherSuite
import com.wire.kalium.logic.data.publicuser.model.UserSearchDetails
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.SearchByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUserResult
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class, SnapshotExtension::class)
class SearchUserViewModelTest {

    @Test
    fun `given addMembersSearchNavArgs are null, when calling the searchUseCase, then excludingMembersOfConversation is null`() = runTest {
        val query = "query"

        val (arrangement, viewModel) = Arrangement()
            .withAddMembersSearchNavArgsThatThrowsException()
            .withSearchResult(
                SearchUserResult(
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
            .withIsValidHandleResult(ValidateUserHandleResult.Invalid.TooLong(""))
            .arrange()

        viewModel.searchQueryChanged(query)
        coVerify(exactly = 1) {
            arrangement.searchUsersUseCase(
                query,
                excludingMembersOfConversation = null,
                customDomain = "domain"
            )
        }

        coVerify(exactly = 1) {
            arrangement.federatedSearchParser(any(), any())
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
                    SearchUserResult(
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
                .withIsValidHandleResult(ValidateUserHandleResult.Invalid.TooLong(""))
                .arrange()

            viewModel.searchQueryChanged(query)

            coVerify(exactly = 1) {
                arrangement.searchUsersUseCase(
                    query,
                    excludingMembersOfConversation = conversationId,
                    customDomain = "domain"
                )
            }

            coVerify(exactly = 1) {
                arrangement.federatedSearchParser(any(), eq(false))
            }
        }

    @Test
    fun `given Proteus conversation and MLS team, when calling the searchUseCase, then otherDomain is not allowed`() =
        runTest {
            val conversationId = ConversationId("id", "domain")
            val (_, viewModel) = Arrangement()
                .withAddMembersSearchNavArgs(AddMembersSearchNavArgs(conversationId, true))
                .withIsFederationSearchAllowedResult(false)
                .withIsValidHandleResult(ValidateUserHandleResult.Valid(""))
                .withFederatedSearchParserResult(
                    FederatedSearchParser.Result(
                        searchTerm = "query",
                        domain = "domain"
                    )
                )
                .withSearchByHandleResult(
                    SearchUserResult(
                        connected = listOf(),
                        notConnected = listOf()
                    )
                )
                .arrange()

            assertEquals(false, viewModel.state.isOtherDomainAllowed)
        }

    @Test
    fun `given MLS conversation and Proteus team, when calling the searchUseCase, then otherDomain is not allowed`() =
        runTest {
            val conversationId = ConversationId("id", "domain")
            val (_, viewModel) = Arrangement()
                .withAddMembersSearchNavArgs(AddMembersSearchNavArgs(conversationId, true))
                .withIsFederationSearchAllowedResult(false)
                .withIsValidHandleResult(ValidateUserHandleResult.Valid(""))
                .withFederatedSearchParserResult(
                    FederatedSearchParser.Result(
                        searchTerm = "query",
                        domain = "domain"
                    )
                )
                .withSearchByHandleResult(
                    SearchUserResult(
                        connected = listOf(),
                        notConnected = listOf()
                    )
                )
                .arrange()

            assertEquals(false, viewModel.state.isOtherDomainAllowed)
        }

    @Test
    fun `given MLS conversation and MLS team, when calling the searchUseCase, then otherDomain is allowed`() =
        runTest {
            val conversationId = ConversationId("id", "domain")
            val (_, viewModel) = Arrangement()
                .withAddMembersSearchNavArgs(AddMembersSearchNavArgs(conversationId, true))
                .withIsFederationSearchAllowedResult(true)
                .withIsValidHandleResult(ValidateUserHandleResult.Valid(""))
                .withFederatedSearchParserResult(
                    FederatedSearchParser.Result(
                        searchTerm = "query",
                        domain = "domain"
                    )
                )
                .withSearchByHandleResult(
                    SearchUserResult(
                        connected = listOf(),
                        notConnected = listOf()
                    )
                )
                .arrange()

            assertEquals(true, viewModel.state.isOtherDomainAllowed)
        }

    @Test
    fun `given searchUserUseCase returns a list of connected users, when calling the searchUseCase, then contactsResult is set`() =
        runTest {

            val result = SearchUserResult(
                connected = listOf(
                    UserSearchDetails(
                        id = UserId("connected", "domain"),
                        name = "connected",
                        completeAssetId = null,
                        type = UserTypeInfo.Regular(UserType.INTERNAL),
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
                        type = UserTypeInfo.Regular(UserType.INTERNAL),
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
                .withIsValidHandleResult(ValidateUserHandleResult.Invalid.TooLong(""))
                .arrange()

            viewModel.searchQueryChanged(query)

            coVerify(exactly = 1) {
                arrangement.searchUsersUseCase(
                    query,
                    excludingMembersOfConversation = null,
                    customDomain = "domain"
                )
            }

            coVerify(exactly = 1) {
                arrangement.federatedSearchParser(any(), any())
            }

            assertEquals(result.connected.map(arrangement::fromSearchUserResult), viewModel.state.contactsResult)
            assertEquals(result.notConnected.map(arrangement::fromSearchUserResult), viewModel.state.publicResult)
        }

    @Test
    fun `given search term is a valid handle, when searching, then search by handle`() = runTest {
        val query = "query"
        val (arrangement, viewModel) = Arrangement()
            .withAddMembersSearchNavArgsThatThrowsException()
            .withSearchByHandleResult(
                SearchUserResult(
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
            .withIsValidHandleResult(ValidateUserHandleResult.Valid(""))
            .arrange()

        viewModel.searchQueryChanged(query)
        coVerify(exactly = 1) {
            arrangement.searchByHandleUseCase.invoke(
                query,
                excludingConversation = null,
                customDomain = "domain"
            )
        }

        coVerify(exactly = 1) {
            arrangement.federatedSearchParser(any(), any())
        }
    }

    @Test
    fun `given a contact is selected, when searching, then return it on selected result list, not on contacts result list`() = runTest {
        val query = ""
        val selectedUserSearchDetails = UserSearchDetails(
            id = UserId("id", "domain"),
            name = "name",
            completeAssetId = null,
            type = UserTypeInfo.Regular(UserType.INTERNAL),
            connectionStatus = ConnectionState.ACCEPTED,
            previewAssetId = null,
            handle = "handle"
        )
        val (arrangement, viewModel) = Arrangement()
            .withAddMembersSearchNavArgsThatThrowsException()
            .withSearchByHandleResult(
                SearchUserResult(
                    connected = listOf(selectedUserSearchDetails),
                    notConnected = emptyList()
                )
            )
            .withFederatedSearchParserResult(FederatedSearchParser.Result(searchTerm = query, domain = "domain"))
            .withIsValidHandleResult(ValidateUserHandleResult.Valid(""))
            .arrange()
        val selectedContact = arrangement.fromSearchUserResult(selectedUserSearchDetails)

        viewModel.searchQueryChanged(query)
        viewModel.selectedContactsChanged(persistentSetOf(selectedContact))
        advanceUntilIdle()

        assertEquals(persistentListOf(selectedContact), viewModel.state.selectedResult)
        assertEquals(persistentListOf<Contact>(), viewModel.state.contactsResult)
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

        @MockK
        lateinit var validateUserHandle: ValidateUserHandleUseCase

        @MockK
        lateinit var searchByHandleUseCase: SearchByHandleUseCase

        @MockK
        lateinit var isFederationSearchAllowedUseCase: IsFederationSearchAllowedUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { contactMapper.fromSearchUserResult(any()) } answers {
                val user = args.get(0) as UserSearchDetails
                fromSearchUserResult(user)
            }
            withIsFederationSearchAllowedResult(false)
        }

        @Suppress("CyclomaticComplexMethod")
        fun fromSearchUserResult(user: UserSearchDetails): Contact {
            with(user) {
                return Contact(
                    id = id.value,
                    domain = id.domain,
                    name = name ?: String.EMPTY,
                    handle = handle ?: String.EMPTY,
                    label = user.handle ?: String.EMPTY,
                    avatarData = UserAvatarData(asset = null),
                    membership = when (val type = user.type) {
                        UserTypeInfo.App -> Membership.Service
                        UserTypeInfo.Bot -> Membership.Service
                        is UserTypeInfo.Regular -> {
                            when (type.type) {
                                UserType.FEDERATED -> Membership.Federated
                                UserType.EXTERNAL -> Membership.External
                                UserType.INTERNAL -> Membership.Standard
                                UserType.NONE -> Membership.None
                                UserType.ADMIN -> Membership.Admin
                                UserType.OWNER -> Membership.Owner
                                UserType.GUEST -> Membership.Guest
                            }
                        }
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

        fun withSearchResult(result: SearchUserResult) = apply {
            coEvery { searchUsersUseCase(any(), any(), any()) } returns result
        }

        fun withFederatedSearchParserResult(result: FederatedSearchParser.Result) = apply {
            coEvery { federatedSearchParser(any(), any()) } returns result
        }

        fun withIsValidHandleResult(result: ValidateUserHandleResult) = apply {
            coEvery { validateUserHandle(any()) } returns result
        }

        fun withSearchByHandleResult(result: SearchUserResult) = apply {
            coEvery { searchByHandleUseCase(any(), any(), any()) } returns result
        }

        fun withIsFederationSearchAllowedResult(isAllowed: Boolean = true) = apply {
            coEvery { isFederationSearchAllowedUseCase(any()) } returns isAllowed
        }

        private lateinit var searchUserViewModel: SearchUserViewModel

        fun arrange() = apply {
            searchUserViewModel = SearchUserViewModel(
                searchUserUseCase = searchUsersUseCase,
                searchByHandleUseCase = searchByHandleUseCase,
                contactMapper = contactMapper,
                federatedSearchParser = federatedSearchParser,
                validateUserHandle = validateUserHandle,
                isFederationSearchAllowed = isFederationSearchAllowedUseCase,
                savedStateHandle = savedStateHandle
            )
        }.run {
            this to searchUserViewModel
        }
    }

    companion object {

        val mlsProtocol = Conversation.ProtocolInfo.MLS(
            GroupID("s"),
            Conversation.ProtocolInfo.MLSCapable.GroupState.PENDING_CREATION,
            0UL,
            Instant.parse("2021-03-30T15:36:00.000Z"),
            cipherSuite = CipherSuite.MLS_128_DHKEMX25519_AES128GCM_SHA256_Ed25519
        )
    }
}
