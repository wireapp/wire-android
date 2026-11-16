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
package com.wire.android.search.users

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.Contact
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.mls.CipherSuite
import com.wire.kalium.logic.data.publicuser.model.UserSearchDetails
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.ContactStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.RefreshContactStatusesUseCase
import com.wire.kalium.logic.feature.search.SearchUserResult
import com.wire.kalium.logic.feature.search.SearchUsersByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchUsersByNameUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Instant

@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class SearchUserViewModelTest {

    private fun pendingTestUser(id: String = "user", remote: Boolean = false) = UserSearchDetails(
        id = UserId(id, "domain"),
        name = "User",
        handle = "user",
        completeAssetId = null,
        previewAssetId = null,
        type = UserTypeInfo.Regular(UserType.INTERNAL),
        connectionStatus = ConnectionState.ACCEPTED,
        contactStatus = if (remote) ContactStatus.NON_CONTACTABLE else null,
    )

    private fun pendingArrangement(users: List<UserSearchDetails>) = Arrangement()
        .withSearchResult(SearchUserResult(connected = users, notConnected = emptyList()))
        .withFederatedSearchParserResult(FederatedSearchParser.Result("", "domain"))
        .withIsValidHandleResult(ValidateUserHandleResult.Invalid.TooLong(""))

    @Test
    fun `disabled feature ignores returned and selected status and does not refresh on resume`() = runTest {
        val user = pendingTestUser(remote = true)
        val arrangement = pendingArrangement(listOf(user)).apply { pendingConnectionsEnabled = false }
        val (_, viewModel) = arrangement.arrange()
        val selected = arrangement.fromSearchUserResult(pendingTestUser("selected")).copy(contactStatus = ContactStatus.NON_CONTACTABLE)
        viewModel.selectedContactsChanged(persistentSetOf(selected))
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.contactsResult.single().isOnboardingPending)
        assertEquals(false, viewModel.state.selectedResult.single().isOnboardingPending)
        viewModel.onBackground()
        viewModel.onForeground()
        advanceUntilIdle()
        coVerify(exactly = 0) { arrangement.refreshContactStatuses(any()) }
        coVerify(exactly = 1) { arrangement.searchUsersByNameUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `blank list shows loaded status throughout an unchanged foreground refresh`() = runTest {
        val user = pendingTestUser()
        val arrangement = pendingArrangement(listOf(user))
        val response = CompletableDeferred<Map<UserId, ContactStatus?>>()
        coEvery { arrangement.refreshContactStatuses(any()) } coAnswers { response.await() }
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.contactsResult.single().isOnboardingPending)
        response.complete(mapOf(user.id to ContactStatus.NON_CONTACTABLE))
        advanceUntilIdle()
        assertEquals(true, viewModel.state.contactsResult.single().isOnboardingPending)
        coVerify(exactly = 1) { arrangement.refreshContactStatuses(setOf(user.id)) }
        val loadedState = viewModel.state
        val refresh = CompletableDeferred<Map<UserId, ContactStatus?>>()
        coEvery { arrangement.refreshContactStatuses(any()) } coAnswers { refresh.await() }
        viewModel.onBackground()
        viewModel.onForeground()
        assertEquals(loadedState, viewModel.state)
        advanceUntilIdle()
        assertEquals(loadedState, viewModel.state)
        refresh.complete(mapOf(user.id to ContactStatus.NON_CONTACTABLE))
        advanceUntilIdle()
        assertEquals(loadedState, viewModel.state)
        coVerify(exactly = 2) { arrangement.refreshContactStatuses(setOf(user.id)) }
    }

    @Test
    fun `foreground refresh updates returned users and retains omitted statuses`() = runTest {
        val changed = pendingTestUser("changed")
        val unchanged = pendingTestUser("unchanged")
        val arrangement = pendingArrangement(listOf(changed, unchanged))
        val initialStatuses = mapOf(changed.id to ContactStatus.NON_CONTACTABLE, unchanged.id to ContactStatus.NON_CONTACTABLE)
        coEvery { arrangement.refreshContactStatuses(any()) } returns initialStatuses
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        val refresh = CompletableDeferred<Map<UserId, ContactStatus?>>()
        coEvery { arrangement.refreshContactStatuses(any()) } coAnswers { refresh.await() }
        viewModel.onBackground()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(initialStatuses, viewModel.state.contactStatuses)
        refresh.complete(mapOf(changed.id to ContactStatus.CONTACTABLE))
        advanceUntilIdle()
        assertEquals(false, viewModel.state.contactsResult.first().isOnboardingPending)
        assertEquals(true, viewModel.state.contactsResult.last().isOnboardingPending)
        coVerify(exactly = 2) { arrangement.refreshContactStatuses(setOf(changed.id, unchanged.id)) }
    }

    @Test
    fun `failed foreground refresh retains selected users status and selection owner snapshot`() = runTest {
        val user = pendingTestUser()
        val arrangement = pendingArrangement(emptyList())
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(user.id to ContactStatus.NON_CONTACTABLE)
        val (_, viewModel) = arrangement.arrange()
        viewModel.selectedContactsChanged(persistentSetOf(arrangement.fromSearchUserResult(user)))
        viewModel.onForeground()
        advanceUntilIdle()
        val loadedState = viewModel.state
        coEvery { arrangement.refreshContactStatuses(any()) } returns emptyMap()
        viewModel.onBackground()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(loadedState, viewModel.state)
        assertEquals(true, viewModel.state.selectedResult.single().isOnboardingPending)
        assertEquals(ContactStatus.NON_CONTACTABLE, viewModel.state.contactStatuses[user.id])
        coVerify(exactly = 2) { arrangement.refreshContactStatuses(setOf(user.id)) }
    }

    @Test
    fun `remote hydrated users are not fetched twice and local results are hydrated`() = runTest {
        val remote = pendingTestUser("remote", remote = true)
        val contactableRemote = pendingTestUser("contactable").copy(contactStatus = ContactStatus.CONTACTABLE)
        val local = pendingTestUser("local")
        val (arrangement, viewModel) = pendingArrangement(listOf(remote, contactableRemote, local)).arrange()
        advanceUntilIdle()
        assertEquals(true, viewModel.state.contactsResult.first().isOnboardingPending)
        assertEquals(false, viewModel.state.contactsResult[1].isOnboardingPending)
        assertEquals(false, viewModel.state.contactsResult.last().isOnboardingPending)
        coVerify(exactly = 1) { arrangement.refreshContactStatuses(setOf(local.id)) }
    }

    @Test
    fun `selected users outside search results get fresh status`() = runTest {
        val user = pendingTestUser()
        val arrangement = pendingArrangement(emptyList())
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(user.id to ContactStatus.NON_CONTACTABLE)
        val (_, viewModel) = arrangement.arrange()
        viewModel.selectedContactsChanged(persistentSetOf(arrangement.fromSearchUserResult(user)))
        advanceUntilIdle()
        assertEquals(true, viewModel.state.selectedResult.single().isOnboardingPending)
        assertEquals(ContactStatus.NON_CONTACTABLE, viewModel.state.contactStatuses[user.id])
        viewModel.selectedContactsChanged(persistentSetOf(viewModel.state.selectedResult.single()))
        advanceUntilIdle()
        coVerify(exactly = 1) { arrangement.refreshContactStatuses(setOf(user.id)) }
    }

    @Test
    fun `late response from a background screen cannot restore pending status`() = runTest {
        val user = pendingTestUser()
        val arrangement = pendingArrangement(listOf(user))
        val response = CompletableDeferred<Map<UserId, ContactStatus?>>()
        coEvery { arrangement.refreshContactStatuses(any()) } coAnswers { response.await() }
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        viewModel.onBackground()
        coEvery { arrangement.refreshContactStatuses(any()) } returns emptyMap()
        viewModel.onForeground()
        advanceUntilIdle()
        response.complete(mapOf(user.id to ContactStatus.NON_CONTACTABLE))
        advanceUntilIdle()
        assertEquals(false, viewModel.state.contactsResult.single().isOnboardingPending)
    }

    @Test
    fun `query reverted within debounce keeps publishing selection and status updates`() = runTest {
        val user = pendingTestUser()
        val arrangement = pendingArrangement(listOf(user))
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(user.id to ContactStatus.NON_CONTACTABLE)
        val (_, viewModel) = arrangement.arrange()
        advanceUntilIdle()
        viewModel.searchQueryChanged("x")
        viewModel.searchQueryChanged("")
        advanceUntilIdle()
        assertEquals(true, viewModel.state.contactsResult.single().isOnboardingPending)
        viewModel.selectedContactsChanged(persistentSetOf(viewModel.state.contactsResult.single()))
        advanceUntilIdle()
        assertEquals(user.id.value, viewModel.state.selectedResult.single().id)
        assertEquals(true, viewModel.state.selectedResult.single().isOnboardingPending)
        assertEquals(emptyList<Any>(), viewModel.state.contactsResult)
    }

    @Test
    fun `given addMembersSearchNavArgs are null, when calling the searchUseCase, then excludingMembersOfConversation is null`() = runTest {
        val query = "query"

        val (arrangement, viewModel) = Arrangement()
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
            arrangement.searchUsersByNameUseCase(
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
                .withConversationId(conversationId)
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
                arrangement.searchUsersByNameUseCase(
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
                .withConversationId(conversationId)
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
                .withConversationId(conversationId)
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
                .withConversationId(conversationId)
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
                arrangement.searchUsersByNameUseCase(
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
            arrangement.searchUsersByHandleUseCase.invoke(
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

    @Test
    fun `given only self team and domain is false, when searching by handle, then search for all users`() = runTest {
        val query = "handle"
        val (arrangement, viewModel) = Arrangement()
            .withOnlySelfTeamAndDomain(false)
            .withSearchByHandleResult(SearchUserResult(connected = listOf(), notConnected = listOf()))
            .withFederatedSearchParserResult(FederatedSearchParser.Result(searchTerm = query, domain = "domain"))
            .withIsValidHandleResult(ValidateUserHandleResult.Valid(""))
            .arrange()

        viewModel.searchQueryChanged(query)
        coVerify(exactly = 1) {
            arrangement.searchUsersByHandleUseCase.invoke(
                searchHandle = query,
                excludingMembersOfConversation = null,
                onlySelfTeamAndDomain = false,
                customDomain = "domain"
            )
        }
    }

    @Test
    fun `given only self team and domain is true, when searching by handle, then only search for self team and domain users`() = runTest {
        val query = "handle"
        val (arrangement, viewModel) = Arrangement()
            .withOnlySelfTeamAndDomain(true)
            .withSearchByHandleResult(SearchUserResult(connected = listOf(), notConnected = listOf()))
            .withFederatedSearchParserResult(FederatedSearchParser.Result(searchTerm = query, domain = "domain"))
            .withIsValidHandleResult(ValidateUserHandleResult.Valid(""))
            .arrange()

        viewModel.searchQueryChanged(query)
        coVerify(exactly = 1) {
            arrangement.searchUsersByHandleUseCase.invoke(
                searchHandle = query,
                excludingMembersOfConversation = null,
                onlySelfTeamAndDomain = true,
                customDomain = "domain"
            )
        }
    }

    @Test
    fun `given only self team and domain is false, when searching by name, then search for all users`() = runTest {
        val query = "Name"
        val (arrangement, viewModel) = Arrangement()
            .withOnlySelfTeamAndDomain(false)
            .withSearchResult(SearchUserResult(connected = listOf(), notConnected = listOf()))
            .withFederatedSearchParserResult(FederatedSearchParser.Result(searchTerm = query, domain = "domain"))
            .withIsValidHandleResult(ValidateUserHandleResult.Invalid.InvalidCharacters("ame", listOf('N')))
            .arrange()

        viewModel.searchQueryChanged(query)
        coVerify(exactly = 1) {
            arrangement.searchUsersByNameUseCase.invoke(
                searchQuery = query,
                excludingMembersOfConversation = null,
                onlySelfTeamAndDomain = false,
                customDomain = "domain"
            )
        }
    }

    @Test
    fun `given only self team and domain is true, when searching by name, then only search for self team and domain users`() = runTest {
        val query = "Name"
        val (arrangement, viewModel) = Arrangement()
            .withOnlySelfTeamAndDomain(true)
            .withSearchResult(SearchUserResult(connected = listOf(), notConnected = listOf()))
            .withFederatedSearchParserResult(FederatedSearchParser.Result(searchTerm = query, domain = "domain"))
            .withIsValidHandleResult(ValidateUserHandleResult.Invalid.InvalidCharacters("ame", listOf('N')))
            .arrange()

        viewModel.searchQueryChanged(query)
        coVerify(exactly = 1) {
            arrangement.searchUsersByNameUseCase.invoke(
                searchQuery = query,
                excludingMembersOfConversation = null,
                onlySelfTeamAndDomain = true,
                customDomain = "domain"
            )
        }
    }

    private class Arrangement {

        @MockK
        lateinit var searchUsersByNameUseCase: SearchUsersByNameUseCase

        @MockK
        lateinit var contactMapper: ContactMapper

        @MockK
        lateinit var federatedSearchParser: FederatedSearchParser

        @MockK
        lateinit var validateUserHandle: ValidateUserHandleUseCase

        @MockK
        lateinit var searchUsersByHandleUseCase: SearchUsersByHandleUseCase

        @MockK
        lateinit var isFederationSearchAllowedUseCase: IsFederationSearchAllowedUseCase

        @MockK
        lateinit var refreshContactStatuses: RefreshContactStatusesUseCase

        var pendingConnectionsEnabled = true

        private var conversationId: ConversationId? = null

        private var onlySelfTeamAndDomain: Boolean = false

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { refreshContactStatuses(any()) } returns emptyMap()
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

        fun withConversationId(conversationId: ConversationId?) = apply {
            this.conversationId = conversationId
        }

        fun withOnlySelfTeamAndDomain(onlySelfTeamAndDomain: Boolean) = apply {
            this.onlySelfTeamAndDomain = onlySelfTeamAndDomain
        }

        fun withSearchResult(result: SearchUserResult) = apply {
            coEvery { searchUsersByNameUseCase(any(), any(), any(), any()) } returns result
        }

        fun withFederatedSearchParserResult(result: FederatedSearchParser.Result) = apply {
            coEvery { federatedSearchParser(any(), any()) } returns result
        }

        fun withIsValidHandleResult(result: ValidateUserHandleResult) = apply {
            coEvery { validateUserHandle(any()) } returns result
        }

        fun withSearchByHandleResult(result: SearchUserResult) = apply {
            coEvery { searchUsersByHandleUseCase(any(), any(), any(), any()) } returns result
        }

        fun withIsFederationSearchAllowedResult(isAllowed: Boolean = true) = apply {
            coEvery { isFederationSearchAllowedUseCase(any()) } returns isAllowed
        }

        private lateinit var searchUserViewModel: SearchUserViewModel

        fun arrange() = apply {
            searchUserViewModel = SearchUserViewModel(
                conversationId = conversationId,
                onlySelfTeamAndDomain = onlySelfTeamAndDomain,
                searchUsersByName = searchUsersByNameUseCase,
                searchUsersByHandle = searchUsersByHandleUseCase,
                contactMapper = contactMapper,
                federatedSearchParser = federatedSearchParser,
                validateUserHandle = validateUserHandle,
                refreshContactStatuses = refreshContactStatuses,
                kaliumConfigs = KaliumConfigs(pendingConnections = pendingConnectionsEnabled),
                isFederationSearchAllowed = isFederationSearchAllowedUseCase,
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
