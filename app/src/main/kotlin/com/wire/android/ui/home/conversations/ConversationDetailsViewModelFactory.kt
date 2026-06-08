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
package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModel
import com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModel
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModel
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationCodeUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.conversation.channel.UpdateChannelAddPermissionUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import com.wire.kalium.logic.util.RandomPassword
import dev.zacsweers.metro.Inject

@Suppress("LongParameterList")
class ConversationDetailsViewModelFactory @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeParticipantsForConversation: ObserveParticipantsForConversationUseCase,
    private val observeSelfUserWithTeam: ObserveSelfUserWithTeamUseCase,
    private val updateConversationReceiptMode: UpdateConversationReceiptModeUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val isMLSEnabled: IsMLSEnabledUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
    private val renameConversation: RenameConversationUseCase,
    private val updateMessageTimer: UpdateMessageTimerUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
    private val updateChannelAddPermission: UpdateChannelAddPermissionUseCase,
    private val qualifiedIdMapper: QualifiedIdMapper,
    private val changeAccessForAppsInConversation: ChangeAccessForAppsInConversationUseCase,
    private val updateConversationAccessRole: UpdateConversationAccessRoleUseCase,
    private val generateGuestRoomLink: GenerateGuestRoomLinkUseCase,
    private val revokeGuestRoomLink: RevokeGuestRoomLinkUseCase,
    private val observeGuestRoomLink: ObserveGuestRoomLinkUseCase,
    private val observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase,
    private val canCreatePasswordProtectedLinks: CanCreatePasswordProtectedLinksUseCase,
    private val syncConversationCode: SyncConversationCodeUseCase,
    private val getDefaultProtocol: GetDefaultProtocolUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val generatePassword: RandomPassword,
) {
    fun groupConversationDetailsViewModel(savedStateHandle: SavedStateHandle) = GroupConversationDetailsViewModel(
        dispatcher = dispatcher,
        observeConversationDetails = observeConversationDetails,
        observeConversationMembers = observeParticipantsForConversation,
        observeSelfUserWithTeam = observeSelfUserWithTeam,
        updateConversationReceiptMode = updateConversationReceiptMode,
        observeSelfDeletionTimerSettingsForConversation = observeSelfDeletionTimerSettingsForConversation,
        observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
        savedStateHandle = savedStateHandle,
        isMLSEnabled = isMLSEnabled,
        refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
        isWireCellsEnabled = isWireCellsEnabled,
    )

    fun groupConversationParticipantsViewModel(savedStateHandle: SavedStateHandle) = GroupConversationParticipantsViewModel(
        savedStateHandle = savedStateHandle,
        observeConversationMembers = observeParticipantsForConversation,
        refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
    )

    fun editConversationMetadataViewModel(savedStateHandle: SavedStateHandle) = EditConversationMetadataViewModel(
        dispatcher = dispatcher,
        observeConversationDetails = observeConversationDetails,
        renameConversation = renameConversation,
        savedStateHandle = savedStateHandle,
    )

    fun editSelfDeletingMessagesViewModel(savedStateHandle: SavedStateHandle) = EditSelfDeletingMessagesViewModel(
        dispatcher = dispatcher,
        observeConversationMembers = observeParticipantsForConversation,
        observeSelfDeletionTimerSettingsForConversation = observeSelfDeletionTimerSettingsForConversation,
        updateMessageTimer = updateMessageTimer,
        selfUser = observeSelfUser,
        conversationDetails = observeConversationDetails,
        savedStateHandle = savedStateHandle,
    )

    fun updateChannelAccessViewModel(savedStateHandle: SavedStateHandle) = UpdateChannelAccessViewModel(
        savedStateHandle = savedStateHandle,
        updateChannelAddPermission = updateChannelAddPermission,
        qualifiedIdMapper = qualifiedIdMapper,
    )

    fun updateAppsAccessViewModel(savedStateHandle: SavedStateHandle) = UpdateAppsAccessViewModel(
        dispatcher = dispatcher,
        observeConversationDetails = observeConversationDetails,
        observeConversationMembers = observeParticipantsForConversation,
        observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
        selfUser = observeSelfUser,
        changeAccessForAppsInConversation = changeAccessForAppsInConversation,
        savedStateHandle = savedStateHandle,
    )

    fun editGuestAccessViewModel(savedStateHandle: SavedStateHandle) = EditGuestAccessViewModel(
        dispatcher = dispatcher,
        updateConversationAccessRole = updateConversationAccessRole,
        observeConversationDetails = observeConversationDetails,
        observeConversationMembers = observeParticipantsForConversation,
        generateGuestRoomLink = generateGuestRoomLink,
        revokeGuestRoomLink = revokeGuestRoomLink,
        observeGuestRoomLink = observeGuestRoomLink,
        observeGuestRoomLinkFeatureFlag = observeGuestRoomLinkFeatureFlag,
        canCreatePasswordProtectedLinks = canCreatePasswordProtectedLinks,
        syncConversationCode = syncConversationCode,
        getDefaultProtocol = getDefaultProtocol,
        selfUser = observeSelfUser,
        savedStateHandle = savedStateHandle,
    )

    fun createPasswordGuestLinkViewModel(savedStateHandle: SavedStateHandle) = CreatePasswordGuestLinkViewModel(
        generateGuestRoomLink = generateGuestRoomLink,
        validatePassword = validatePassword,
        generatePassword = generatePassword,
        savedStateHandle = savedStateHandle,
    )

    fun checkAssetRestrictionsViewModel() = CheckAssetRestrictionsViewModel()
}
