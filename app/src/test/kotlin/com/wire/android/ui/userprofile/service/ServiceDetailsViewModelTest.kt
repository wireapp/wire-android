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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModelTest
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.service.ServiceDetails
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
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
                .withService(service = BOT_SERVICE)
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
    fun `given user opens service details screen, when service is not member of conversation, then data is loaded correctly`() =
        runTest {
            // given
            val (_, viewModel) = Arrangement()
                .withService(service = BOT_SERVICE)
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
                .withService(service = BOT_SERVICE)
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
                .withService(service = BOT_SERVICE)
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
                .withService(service = BOT_SERVICE)
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
                .withService(service = BOT_SERVICE)
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
                .withService(service = BOT_SERVICE)
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
                .withService(service = BOT_SERVICE)
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

    companion object {
        const val serviceId = "serviceId"
        const val providerId = "providerId"
        val SERVICE_ID = ServiceId(id = serviceId, provider = providerId)
        val BOT_SERVICE = BotService(id = serviceId, provider = providerId)
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
        val CONVERSATION_ROLE_DATA = ConversationRoleData(
            "some_name",
            Conversation.Member.Role.Admin,
            Conversation.Member.Role.Admin,
            OtherUserProfileScreenViewModelTest.CONVERSATION_ID
        )
    }

    private class Arrangement {

        @MockK
        lateinit var getServiceById: GetServiceByIdUseCase

        @MockK
        lateinit var observeIsServiceMember: ObserveIsServiceMemberUseCase

        @MockK
        lateinit var observeConversationRoleForUser: ObserveConversationRoleForUserUseCase

        @MockK
        lateinit var removeMemberFromConversation: RemoveMemberFromConversationUseCase

        @MockK
        lateinit var addServiceToConversation: AddServiceToConversationUseCase

        val serviceDetailsMapper: ServiceDetailsMapper = ServiceDetailsMapper()

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        private val selfUser = TestUser.SELF_USER

        private val viewModel by lazy {
            ServiceDetailsViewModel(
                TestDispatcherProvider(),
                selfUserId = selfUser.id,
                getServiceById,
                observeIsServiceMember,
                observeConversationRoleForUser,
                removeMemberFromConversation,
                addServiceToConversation,
                serviceDetailsMapper,
                savedStateHandle
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()
        }

        fun withService(service: BotService) = apply {
            every { savedStateHandle.navArgs<ServiceDetailsNavArgs>() } returns ServiceDetailsNavArgs(service, CONVERSATION_ID)
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
