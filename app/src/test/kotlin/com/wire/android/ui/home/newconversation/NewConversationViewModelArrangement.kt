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

package com.wire.android.ui.home.newconversation

import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionState
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCaseImpl
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK

internal class NewConversationViewModelArrangement {
    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()

        // Default empty values
        coEvery { isMLSEnabledUseCase() } returns true
        coEvery { createGroupConversation(any(), any(), any()) } returns CreateGroupConversationUseCase.Result.Success(CONVERSATION)
        every { getDefaultProtocol() } returns SupportedProtocol.PROTEUS
    }

    @MockK
    lateinit var createGroupConversation: CreateGroupConversationUseCase

    @MockK
    lateinit var isMLSEnabledUseCase: IsMLSEnabledUseCase

    @MockK
    lateinit var isSelfTeamMember: IsSelfATeamMemberUseCaseImpl

    @MockK(relaxed = true)
    lateinit var onGroupCreated: (ConversationId) -> Unit

    @MockK
    lateinit var getDefaultProtocol: GetDefaultProtocolUseCase

    private var groupOptionsState: GroupOptionState = GroupOptionState()

    private var createGroupState: CreateGroupState = CreateGroupState()

    private companion object {
        val CONVERSATION_ID = ConversationId(value = "userId", domain = "domainId")
        val CONVERSATION = Conversation(
            id = CONVERSATION_ID,
            name = null,
            type = Conversation.Type.ONE_ON_ONE,
            teamId = null,
            protocol = Conversation.ProtocolInfo.Proteus,
            mutedStatus = MutedConversationStatus.AllAllowed,
            removedBy = null,
            lastNotificationDate = null,
            lastModifiedDate = null,
            lastReadDate = "2022-04-04T16:11:28.388Z",
            access = listOf(Conversation.Access.INVITE),
            accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER),
            creatorId = null,
            receiptMode = Conversation.ReceiptMode.ENABLED,
            messageTimer = null,
            userMessageTimer = null,
            archived = false,
            archivedDateTime = null,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
        )

        val PUBLIC_USER = OtherUser(
            TestUser.USER_ID.copy(value = "publicValue"),
            name = "publicUsername",
            handle = "publicHandle",
            email = "publicEmail",
            phone = "publicPhone",
            accentId = 0,
            teamId = TeamId("publicTeamId"),
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = UserAssetId("value", "domain"),
            completePicture = UserAssetId("value", "domain"),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            userType = UserType.FEDERATED,
            botService = null,
            deleted = false,
            defederated = false,
            isProteusVerified = false,
            supportedProtocols = setOf(SupportedProtocol.PROTEUS)
        )

        val FEDERATED_KNOWN_USER = OtherUser(
            TestUser.USER_ID.copy(value = "knownValue"),
            name = "knownUsername",
            handle = "knownHandle",
            email = "knownEmail",
            phone = "knownPhone",
            accentId = 0,
            teamId = TeamId("knownTeamId"),
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = UserAssetId("value", "domain"),
            completePicture = UserAssetId("value", "domain"),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            userType = UserType.FEDERATED,
            botService = null,
            deleted = false,
            defederated = false,
            isProteusVerified = false,
            supportedProtocols = setOf(SupportedProtocol.PROTEUS)
        )
    }

    fun withSyncFailureOnCreatingGroup() = apply {
        coEvery { createGroupConversation(any(), any(), any()) } returns CreateGroupConversationUseCase.Result.SyncFailure
    }

    fun withUnknownFailureOnCreatingGroup() = apply {
        coEvery { createGroupConversation(any(), any(), any()) } returns CreateGroupConversationUseCase.Result.UnknownFailure(
            CoreFailure.MissingClientRegistration
        )
    }

    fun withConflictingBackendsFailure() = apply {
        createGroupState = createGroupState.copy(
            error = CreateGroupState.Error.ConflictedBackends(listOf("bella.wire.link", "foma.wire.link"))
        )
    }

    fun withIsSelfTeamMember(result: Boolean) = apply {
        coEvery { isSelfTeamMember() } returns result
    }

    fun withGuestEnabled(isGuestModeEnabled: Boolean) = apply {
        groupOptionsState = groupOptionsState.copy(isAllowGuestEnabled = isGuestModeEnabled)
    }

    fun withServicesEnabled(areServicesEnabled: Boolean) = apply {
        groupOptionsState = groupOptionsState.copy(isAllowServicesEnabled = areServicesEnabled)
    }

    fun withDefaultProtocol(supportedProtocol: SupportedProtocol) = apply {
        every { getDefaultProtocol() } returns supportedProtocol
    }

    fun arrange() = this to NewConversationViewModel(
        createGroupConversation = createGroupConversation,
        isMLSEnabled = isMLSEnabledUseCase,
        isSelfATeamMember = isSelfTeamMember,
        getDefaultProtocol = getDefaultProtocol
    ).also {
        it.groupOptionsState = groupOptionsState
        it.createGroupState = createGroupState
    }
}
