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
package com.wire.android.ui.home.conversations.search.apps

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.mls.CipherSuite
import com.wire.kalium.logic.data.service.ServiceDetails
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.feature.app.ObserveAllAppsUseCase
import com.wire.kalium.logic.feature.app.SearchAppsByNameUseCase
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedProtocol
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class SearchAppsViewModelTest {

    @Test
    fun `given apps feature flag is disabled, when init view model, then loading is finished and result is empty`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .arrange(protocolInfo = null)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            assertTrue(viewModel.state.result.isEmpty())
            assertFalse(viewModel.state.isLoading)
        }

    @Test
    fun `given Apps feature flag is MLS enabled, when init view model, then load existing Apps`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withGetAllApps(listOf(SERVICE_DETAILS))
                .arrange(protocolInfo = null)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.getAllApps()
            }
            assertEquals(1, viewModel.state.result.size)
        }

    @Test
    fun `given Apps feature flag is PROTEUS enabled, when init view model, then load existing Services`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
                .withGetAllServices(listOf(SERVICE_DETAILS))
                .arrange(protocolInfo = null)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.getAllServices()
            }
            assertEquals(1, viewModel.state.result.size)
        }

    @Test
    fun `given AppsAllowedProtocol is MIXED, when init view model, then load existing Apps based on conversation protocol (MLS)`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.MLS)))
                .withGetAllApps(listOf(SERVICE_DETAILS, SERVICE_DETAILS2))
                .arrange(protocolInfo = TestConversation.MLS_PROTOCOL_INFO)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.getAllApps()
            }
            assertEquals(2, viewModel.state.result.size)
        }

    @Test
    fun `given AppsAllowedProtocol is MIXED, when init view model, then load existing Apps based on conversation protocol (Proteus)`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)))
                .withGetAllServices(listOf(SERVICE_DETAILS, SERVICE_DETAILS2))
                .arrange(protocolInfo = Conversation.ProtocolInfo.Proteus)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.getAllServices()
            }
            assertEquals(2, viewModel.state.result.size)
        }

    @Test
    fun `given AppsAllowedProtocol is MIXED and defaultProtocol is Proteus, when init view model, then load existing Services based on conversation protocol (Mixed)`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)))
                .withGetAllServices(listOf(SERVICE_DETAILS, SERVICE_DETAILS2))
                .arrange(protocolInfo = MIXED_PROTOCOL_INFO)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.getAllServices()
            }
            assertEquals(2, viewModel.state.result.size)
        }

    @Test
    fun `given AppsAllowedProtocol is MIXED and defaultProtocol is Proteus, when init view model, then load existing Services based on new conversation`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)))
                .withGetAllServices(listOf(SERVICE_DETAILS, SERVICE_DETAILS2))
                .arrange(protocolInfo = null)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.getAllServices()
            }
            assertEquals(2, viewModel.state.result.size)
        }

    @Test
    fun `given AppsAllowedProtocol is MIXED and defaultProtocol is MLS, when init view model, then load existing Apps based on new conversation`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.MLS)))
                .withGetAllApps(listOf(SERVICE_DETAILS, SERVICE_DETAILS2))
                .arrange(protocolInfo = null)

            // when
            // viewModel is initialized
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.getAllApps()
            }
            assertEquals(2, viewModel.state.result.size)
        }

    @Test
    fun `given Apps feature flag is MLS enabled, when searching for Apps by name, then load searched Apps`() =
        runTest {
            // given
            val query = "Service Name 2"
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withGetAllApps(listOf(SERVICE_DETAILS, SERVICE_DETAILS2))
                .withSearchAppsByName(query, listOf(SERVICE_DETAILS2))
                .arrange(protocolInfo = null)

            // when
            // viewModel is initialized
            advanceUntilIdle()
            viewModel.searchQueryChanged(query)
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.searchAppsByName(query)
            }
            assertEquals(1, viewModel.state.result.size)
        }

    @Test
    fun `given Apps feature flag is PROTEUS enabled, when searching for Apps by name, then load searched Services`() =
        runTest {
            // given
            val query = "Service Name 2"
            val (arrangement, viewModel) = Arrangement()
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
                .withGetAllServices(listOf(SERVICE_DETAILS, SERVICE_DETAILS2))
                .withSearchServicesByName(query, listOf(SERVICE_DETAILS2))
                .arrange(protocolInfo = null)

            // when
            // viewModel is initialized
            advanceUntilIdle()
            viewModel.searchQueryChanged(query)
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) {
                arrangement.searchServicesByName(query)
            }
            assertEquals(1, viewModel.state.result.size)
        }

    private companion object {
        val SERVICE_ID = ServiceId(id = "serviceId", provider = "providerId")
        val SERVICE_ID2 = ServiceId(id = "serviceId2", provider = "providerId2")
        val SERVICE_DETAILS = ServiceDetails(
            id = SERVICE_ID,
            name = "Service Name",
            description = "Service Description",
            summary = "Service Summary",
            enabled = true,
            tags = listOf(),
            previewAssetId = null,
            completeAssetId = null
        )
        val SERVICE_DETAILS2 = SERVICE_DETAILS.copy(
            id = SERVICE_ID2,
            name = "${SERVICE_DETAILS.name} 2"
        )
        val DUMMY_CONTACT = Contact(
            id = SERVICE_ID.id,
            domain = SERVICE_ID.provider,
            name = SERVICE_DETAILS.name,
            handle = String.EMPTY,
            label = String.EMPTY,
            avatarData = UserAvatarData(
                asset = SERVICE_DETAILS.previewAssetId?.let { ImageAsset.UserAvatarAsset(it) },
                membership = Membership.Service
            ),
            membership = Membership.Service,
            connectionState = ConnectionState.ACCEPTED
        )
        val DUMMY_CONTACT2 = DUMMY_CONTACT.copy(
            id = SERVICE_ID2.id,
            domain = SERVICE_ID2.provider,
            name = SERVICE_DETAILS2.name
        )

        val MIXED_PROTOCOL_INFO = Conversation.ProtocolInfo.Mixed(
            groupId = GroupID("groupId"),
            groupState = Conversation.ProtocolInfo.MLSCapable.GroupState.ESTABLISHED,
            epoch = 1.toULong(),
            cipherSuite = CipherSuite.Companion.fromTag(1),
            keyingMaterialLastUpdate = kotlinx.datetime.Instant.DISTANT_PAST
        )
    }

    private class Arrangement {

        @MockK
        lateinit var getAllServices: ObserveAllServicesUseCase

        @MockK
        lateinit var getAllApps: ObserveAllAppsUseCase

        @MockK
        lateinit var contactMapper: ContactMapper

        @MockK
        lateinit var searchServicesByName: SearchServicesByNameUseCase

        @MockK
        lateinit var searchAppsByName: SearchAppsByNameUseCase

        @MockK
        lateinit var isAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase

        @MockK
        lateinit var observeSelfUser: ObserveSelfUserUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { getAllServices() } returns flowOf(emptyList())
            coEvery { getAllApps() } returns flowOf(emptyList())
            coEvery { searchServicesByName(any()) } returns flowOf(emptyList())
            coEvery { searchAppsByName(any()) } returns flowOf(emptyList())
            coEvery { isAppsAllowedForUsage() } returns flowOf(AppsAllowedResult.Disabled)
            coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)

            every { contactMapper.fromService(SERVICE_DETAILS) } returns DUMMY_CONTACT
            every { contactMapper.fromService(SERVICE_DETAILS2) } returns DUMMY_CONTACT2
        }

        fun arrange(protocolInfo: Conversation.ProtocolInfo?) = this to SearchAppsViewModel(
            protocolInfo = protocolInfo,
            getAllServices = getAllServices,
            getAllApps = getAllApps,
            contactMapper = contactMapper,
            searchServicesByName = searchServicesByName,
            searchAppsByName = searchAppsByName,
            isAppsAllowedForUsage = isAppsAllowedForUsage,
            observeSelfUser = observeSelfUser
        )

        fun withAppsAllowedForUsage(result: AppsAllowedResult) = apply {
            coEvery { isAppsAllowedForUsage() } returns flowOf(result)
        }

        fun withGetAllApps(result: List<ServiceDetails>) = apply {
            coEvery { getAllApps() } returns flowOf(result)
        }

        fun withGetAllServices(result: List<ServiceDetails>) = apply {
            coEvery { getAllServices() } returns flowOf(result)
        }

        fun withSearchAppsByName(query: String, result: List<ServiceDetails>) = apply {
            coEvery { searchAppsByName(query) } returns flowOf(result)
        }

        fun withSearchServicesByName(query: String, result: List<ServiceDetails>) = apply {
            coEvery { searchServicesByName(query) } returns flowOf(result)
        }
    }
}
