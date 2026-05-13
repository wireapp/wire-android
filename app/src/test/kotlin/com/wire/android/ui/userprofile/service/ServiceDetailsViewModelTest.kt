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
package com.wire.android.ui.userprofile.service

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.service.ServiceDetails
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberResult
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedProtocol
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberResult
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class ServiceDetailsViewModelTest {

    @Test
    fun `given user opens service details screen, when service is member of conversation, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(null, viewModel.serviceDetailsState.serviceAvatarAsset)
            assertEquals(SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(MEMBER_ID, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.REMOVE, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when apps allowed is Proteus protocol, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(null, viewModel.serviceDetailsState.serviceAvatarAsset)
            assertEquals(SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(MEMBER_ID, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.REMOVE, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when apps allowed is Mixed and default protocol is Proteus, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(null, viewModel.serviceDetailsState.serviceAvatarAsset)
            assertEquals(SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(MEMBER_ID, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.REMOVE, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when service is not member of conversation, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Failure(StorageFailure.DataNotFound))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(null, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.ADD, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when user is not admin, then buttons are hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(
                    roleData = CONVERSATION_ROLE_DATA.copy(
                        userRole = Conversation.Member.Role.Member,
                        selfRole = Conversation.Member.Role.Member
                    )
                )
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(MEMBER_ID, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.HIDDEN, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when service doesn't exist, then service not found screen is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = null)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Failure(StorageFailure.DataNotFound))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(null, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(null, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.HIDDEN, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when removing service successfully, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .withRemoveService(result = RemoveMemberFromConversationUseCase.Result.Success)
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.removeService()

                // then
                coVerify(exactly = 1) {
                    arrangement.removeMemberFromConversation(
                        conversationId = CONVERSATION_ID,
                        userIdToRemove = MEMBER_ID
                    )
                }
                assertEquals(ServiceDetailsInfoMessageType.SuccessRemoveService.uiText, awaitItem())
            }
        }

    @Test
    fun `given user opens service details screen, when removing service fails, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .withRemoveService(result = RemoveMemberFromConversationUseCase.Result.Failure(cause = CoreFailure.Unknown(null)))
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.removeService()

                // then
                coVerify(exactly = 1) {
                    arrangement.removeMemberFromConversation(
                        conversationId = CONVERSATION_ID,
                        userIdToRemove = MEMBER_ID
                    )
                }
                assertEquals(ServiceDetailsInfoMessageType.ErrorRemoveService.uiText, awaitItem())
            }
        }

    @Test
    fun `given user opens service details screen, when adding service successfully, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .withAddService(result = AddServiceToConversationUseCase.Result.Success)
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.addService()

                // then
                coVerify(exactly = 1) {
                    arrangement.addServiceToConversation(
                        conversationId = CONVERSATION_ID,
                        serviceId = SERVICE_ID
                    )
                }
                assertEquals(ServiceDetailsInfoMessageType.SuccessAddService.uiText, awaitItem())
            }
        }

    @Test
    fun `given user opens service details screen, when adding service fails, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceBot(service = BOT_SERVICE)
                .withServiceDetails(serviceDetails = SERVICE_DETAILS)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withIsServiceMember(memberResult = ObserveIsServiceMemberResult.Success(MEMBER_ID))
                .withAddService(result = AddServiceToConversationUseCase.Result.Failure(cause = CoreFailure.Unknown(null)))
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.addService()

                // then
                coVerify(exactly = 1) {
                    arrangement.addServiceToConversation(
                        conversationId = CONVERSATION_ID,
                        serviceId = SERVICE_ID
                    )
                }
                assertEquals(ServiceDetailsInfoMessageType.ErrorAddService.uiText, awaitItem())
            }
        }

    @Test
    fun `given user opens service details screen, when app is member of conversation, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .withIsAppMember(ObserveIsAppMemberResult.Success(APP_ID))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(null, viewModel.serviceDetailsState.serviceAvatarAsset)
            assertEquals(APP_SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(APP_ID, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.REMOVE, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when app is not member of conversation, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.MLS)))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .withIsAppMember(ObserveIsAppMemberResult.Failure(StorageFailure.DataNotFound))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(APP_SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(null, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.ADD, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when is from create a conversation flow, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceApp(
                    service = APP_ID,
                    conversationId = null
                )
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.MLS)))
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(APP_SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(null, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.HIDDEN, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen for an app, when user is not admin, then buttons are hidden`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(
                    roleData = CONVERSATION_ROLE_DATA.copy(
                        userRole = Conversation.Member.Role.Member,
                        selfRole = Conversation.Member.Role.Member
                    )
                )
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .withIsAppMember(ObserveIsAppMemberResult.Success(APP_ID))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(APP_SERVICE_DETAILS, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(APP_ID, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.HIDDEN, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when app doesn't exist, then service not found screen is shown`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, null)
                .withIsAppMember(ObserveIsAppMemberResult.Failure(StorageFailure.DataNotFound))
                .arrange()

            // when
            // view model is initialized

            // then
            assertEquals(null, viewModel.serviceDetailsState.serviceDetails)
            assertEquals(null, viewModel.serviceDetailsState.serviceMemberId)
            assertEquals(ServiceDetailsButtonState.HIDDEN, viewModel.serviceDetailsState.buttonState)
        }

    @Test
    fun `given user opens service details screen, when removing app successfully, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .withIsAppMember(ObserveIsAppMemberResult.Success(APP_ID))
                .withRemoveService(result = RemoveMemberFromConversationUseCase.Result.Success)
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.removeService()

                // then
                coVerify(exactly = 1) {
                    arrangement.removeMemberFromConversation(
                        conversationId = CONVERSATION_ID,
                        userIdToRemove = APP_ID
                    )
                }

                assertEquals(ServiceDetailsInfoMessageType.SuccessRemoveService.uiText, awaitItem())
            }
        }

    @Test
    fun `given user opens service details screen, when removing app fails, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .withIsAppMember(ObserveIsAppMemberResult.Success(APP_ID))
                .withRemoveService(result = RemoveMemberFromConversationUseCase.Result.Failure(cause = CoreFailure.Unknown(null)))
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.removeService()

                // then
                coVerify(exactly = 1) {
                    arrangement.removeMemberFromConversation(
                        conversationId = CONVERSATION_ID,
                        userIdToRemove = APP_ID
                    )
                }

                assertEquals(ServiceDetailsInfoMessageType.ErrorRemoveService.uiText, awaitItem())
            }
        }

    @Test
    fun `given user opens service details screen, when adding app successfully, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .withIsAppMember(ObserveIsAppMemberResult.Success(APP_ID))
                .withAddApp(AddMemberToConversationUseCase.Result.Success)
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.addService()

                // then
                coVerify(exactly = 1) {
                    arrangement.addMemberToConversation(
                        conversationId = CONVERSATION_ID,
                        userIdList = listOf(APP_ID)
                    )
                }

                assertEquals(ServiceDetailsInfoMessageType.SuccessAddService.uiText, awaitItem())
            }
        }

    @Test
    fun `given user opens service details screen, when adding app fails, then request is sent correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = Arrangement()
                .withServiceApp(APP_ID)
                .withAppsAllowedForUsage(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
                .withConversationDetails(CONVERSATION_ID)
                .withConversationRoleForUser(roleData = CONVERSATION_ROLE_DATA)
                .withAppDetails(APP_ID, APP_SERVICE_DETAILS)
                .withIsAppMember(ObserveIsAppMemberResult.Success(APP_ID))
                .withAddApp(AddMemberToConversationUseCase.Result.Failure(CoreFailure.Unknown(null)))
                .arrange()

            // when
            viewModel.infoMessage.test {
                viewModel.addService()

                // then
                coVerify(exactly = 1) {
                    arrangement.addMemberToConversation(
                        conversationId = CONVERSATION_ID,
                        userIdList = listOf(APP_ID)
                    )
                }

                assertEquals(ServiceDetailsInfoMessageType.ErrorAddService.uiText, awaitItem())
            }
        }

    companion object {
        const val serviceId = "serviceId"
        const val providerId = "providerId"
        val SERVICE_ID = ServiceId(id = serviceId, provider = providerId)
        val BOT_SERVICE = BotService(id = serviceId, provider = providerId)
        val APP_ID = UserId(value = serviceId, domain = "wire.com")
        val CONVERSATION_ID = ConversationId(value = "conversationId", domain = "conversationDomain")
        val MEMBER_ID = QualifiedID(value = "memberValue", domain = "memberDomain")
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

        val APP_SERVICE_DETAILS = SERVICE_DETAILS.copy(
            id = ServiceId(
                APP_ID.value,
                APP_ID.domain
            )
        )

        val CONVERSATION_ROLE_DATA = ConversationRoleData(
            "some_name",
            Conversation.Member.Role.Admin,
            Conversation.Member.Role.Admin,
            CONVERSATION_ID
        )

        val CONVERSATION_GROUP = TestConversationDetails.GROUP.copy(
            conversation = TestConversation.GROUP().copy(
                id = CONVERSATION_ID,
                protocol = TestConversation.MLS_PROTOCOL_INFO,
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply {
                    add(Conversation.AccessRole.GUEST)
                    add(Conversation.AccessRole.SERVICE)
                }
            )
        )
    }

    private class Arrangement {

        @MockK
        lateinit var getServiceById: GetServiceByIdUseCase

        @MockK
        lateinit var getAppById: GetAppByIdUseCase

        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var observeIsServiceMember: ObserveIsServiceMemberUseCase

        @MockK
        lateinit var observeIsAppMember: ObserveIsAppMemberUseCase

        @MockK
        lateinit var observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase

        @MockK
        lateinit var observeConversationRoleForUser: ObserveConversationRoleForUserUseCase

        @MockK
        lateinit var removeMemberFromConversation: RemoveMemberFromConversationUseCase

        @MockK
        lateinit var addServiceToConversation: AddServiceToConversationUseCase

        @MockK
        lateinit var addMemberToConversation: AddMemberToConversationUseCase

        @MockK
        lateinit var serviceDetailsNavArgsProvider: ServiceDetailsNavArgsProvider

        private val selfUser = TestUser.SELF_USER

        private val viewModel by lazy {
            ServiceDetailsViewModel(
                TestDispatcherProvider(),
                selfUserId = selfUser.id,
                getServiceById,
                getAppById,
                observeConversationDetails,
                observeIsServiceMember,
                observeIsAppMember,
                observeIsAppsAllowedForUsage,
                observeConversationRoleForUser,
                removeMemberFromConversation,
                addServiceToConversation,
                addMemberToConversation,
                serviceDetailsNavArgsProvider
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()
            coEvery {
                observeIsAppMember(any(), any())
            } returns flowOf(ObserveIsAppMemberResult.Success(null))
            coEvery {
                observeIsAppsAllowedForUsage()
            } returns flowOf(AppsAllowedResult.Disabled)
            coEvery {
                observeConversationDetails(any())
            } returns flowOf(ObserveConversationDetailsUseCase.Result.Success(CONVERSATION_GROUP))
        }

        fun withServiceBot(service: BotService, conversationId: ConversationId? = CONVERSATION_ID) = apply {
            every { serviceDetailsNavArgsProvider.serviceDetailsNavArgs() } returns ServiceDetailsNavArgs(
                conversationId,
                ServiceDetailsNavArgs.Id.BotServiceId(service)
            )
        }

        fun withServiceApp(service: UserId, conversationId: ConversationId? = CONVERSATION_ID) = apply {
            every { serviceDetailsNavArgsProvider.serviceDetailsNavArgs() } returns ServiceDetailsNavArgs(
                conversationId,
                ServiceDetailsNavArgs.Id.AppId(service)
            )
        }

        fun withAppsAllowedForUsage(result: AppsAllowedResult) = apply {
            coEvery { observeIsAppsAllowedForUsage() } returns flowOf(result)
        }

        fun withConversationDetails(conversationId: ConversationId) = apply {
            coEvery { observeConversationDetails(conversationId) } returns flowOf(
                ObserveConversationDetailsUseCase.Result.Success(CONVERSATION_GROUP)
            )
        }

        fun withIsAppMember(memberResult: ObserveIsAppMemberResult) = apply {
            coEvery {
                observeIsAppMember(any(), any())
            } returns flowOf(memberResult)
        }

        fun withAppDetails(appId: UserId, serviceDetails: ServiceDetails?) = apply {
            coEvery {
                getAppById(appId)
            } returns serviceDetails
        }

        fun withAddApp(result: AddMemberToConversationUseCase.Result) = apply {
            coEvery {
                addMemberToConversation(any(), any())
            } returns result
        }

        fun withConversationRoleForUser(roleData: ConversationRoleData) = apply {
            coEvery {
                observeConversationRoleForUser.invoke(any(), any())
            } returns flowOf(roleData)
        }

        fun withServiceDetails(serviceDetails: ServiceDetails?) = apply {
            coEvery { getServiceById(any()) } returns serviceDetails
        }

        fun withIsServiceMember(memberResult: ObserveIsServiceMemberResult) = apply {
            coEvery { observeIsServiceMember(any(), any()) } returns flowOf(memberResult)
        }

        fun withRemoveService(result: RemoveMemberFromConversationUseCase.Result) = apply {
            coEvery { removeMemberFromConversation(any(), any()) } returns result
        }

        fun withAddService(result: AddServiceToConversationUseCase.Result) = apply {
            coEvery { addServiceToConversation(any(), any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
