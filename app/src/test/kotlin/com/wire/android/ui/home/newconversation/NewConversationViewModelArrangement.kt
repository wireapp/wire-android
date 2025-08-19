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
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.channels.ChannelCreationPermission
import com.wire.kalium.logic.feature.channels.ObserveChannelsCreationPermissionUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.ConversationCreationResult
import com.wire.kalium.logic.feature.conversation.createconversation.CreateChannelUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateRegularGroupUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant

internal class NewConversationViewModelArrangement {
    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()

        // Default empty values
        coEvery { isMLSEnabledUseCase() } returns true
        coEvery { createRegularGroup(any(), any(), any()) } returns ConversationCreationResult.Success(CONVERSATION)
        coEvery { observeChannelsCreationPermissionUseCase() } returns flowOf(ChannelCreationPermission.Forbidden)
        every { getDefaultProtocol() } returns SupportedProtocol.PROTEUS
        every { globalDataStore.wireCellsEnabled() } returns flowOf(false)
        withAppsAllowedResult(false)
    }

    @MockK
    lateinit var createRegularGroup: CreateRegularGroupUseCase

    @MockK
    lateinit var createChannel: CreateChannelUseCase

    @MockK
    lateinit var isMLSEnabledUseCase: IsMLSEnabledUseCase

    @MockK
    lateinit var observeChannelsCreationPermissionUseCase: ObserveChannelsCreationPermissionUseCase

    @MockK
    lateinit var getSelf: GetSelfUserUseCase

    @MockK
    lateinit var getDefaultProtocol: GetDefaultProtocolUseCase

    @MockK
    lateinit var observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase

    @MockK
    lateinit var globalDataStore: GlobalDataStore

    private var createGroupState: CreateGroupState = CreateGroupState.Default

    internal companion object {
        val CONVERSATION_ID = ConversationId(value = "userId", domain = "domainId")
        val CONVERSATION = Conversation(
            id = CONVERSATION_ID,
            name = null,
            type = Conversation.Type.OneOnOne,
            teamId = null,
            protocol = Conversation.ProtocolInfo.Proteus,
            mutedStatus = MutedConversationStatus.AllAllowed,
            removedBy = null,
            lastNotificationDate = null,
            lastModifiedDate = null,
            lastReadDate = Instant.parse("2022-04-04T16:11:28.388Z"),
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

        val SELF_USER = SelfUser(
            TestUser.USER_ID,
            name = "username",
            handle = "handle",
            email = "email",
            phone = "phone",
            accentId = 0,
            teamId = TeamId("teamId"),
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = UserAssetId("value", "domain"),
            completePicture = UserAssetId("value", "domain"),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            userType = UserType.INTERNAL,
            supportedProtocols = setOf(SupportedProtocol.PROTEUS),
        )
    }

    fun withChannelCreationPermissionReturning(flow: Flow<ChannelCreationPermission>) = apply {
        coEvery { observeChannelsCreationPermissionUseCase() } returns flow
    }

    fun withSyncFailureOnCreatingGroup() = apply {
        coEvery { createRegularGroup(any(), any(), any()) } returns ConversationCreationResult.SyncFailure
    }

    fun withUnknownFailureOnCreatingGroup() = apply {
        coEvery { createRegularGroup(any(), any(), any()) } returns ConversationCreationResult.UnknownFailure(
            CoreFailure.MissingClientRegistration
        )
    }

    fun withConflictingBackendsFailure() = apply {
        createGroupState = CreateGroupState.Error.ConflictedBackends(listOf("bella.wire.link", "foma.wire.link"))
    }

    fun withGetSelfUser(isTeamMember: Boolean, userType: UserType = UserType.INTERNAL) = apply {
        coEvery { getSelf() } returns SELF_USER.copy(
            teamId = if (isTeamMember) TeamId("teamId") else null,
            userType = userType,
        )
    }

    fun withCreateChannelSuccess() = apply {
        coEvery { createChannel(any(), any(), any()) } returns ConversationCreationResult.Success(CONVERSATION)
    }

    fun withCreateChannelFailure() = apply {
        coEvery { createChannel(any(), any(), any()) } returns ConversationCreationResult.SyncFailure
    }

    fun withDefaultProtocol(supportedProtocol: SupportedProtocol) = apply {
        every { getDefaultProtocol() } returns supportedProtocol
    }

    fun withAppsAllowedResult(result: Boolean) = apply {
        coEvery { observeIsAppsAllowedForUsage() } returns flowOf(result)
    }

    fun arrange() = this to NewConversationViewModel(
        createRegularGroup = createRegularGroup,
        createChannel = createChannel,
        isUserAllowedToCreateChannels = observeChannelsCreationPermissionUseCase,
        getSelfUser = getSelf,
        getDefaultProtocol = getDefaultProtocol,
        observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
        globalDataStore = globalDataStore,
    ).also {
        it.createGroupState = createGroupState
    }
}
