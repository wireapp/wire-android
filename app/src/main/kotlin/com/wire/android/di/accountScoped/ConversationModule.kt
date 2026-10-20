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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.conversation.FetchConversationUseCase
import com.wire.kalium.logic.data.conversation.ResetMLSConversationUseCase
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumConversationApi
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationLeaveConditionsUseCase
import com.wire.kalium.logic.feature.conversation.CheckOneToOneConversationIsReadyUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ClearUsersTypingEventsUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationProtocolInfoUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithMembersBySearchQueryUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.IsSelfUserViewerOnConversationUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.MarkConversationAsReadLocallyUseCase
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.MigrateConversationToMLSUseCase
import com.wire.kalium.logic.feature.conversation.NotifyConversationIsOpenUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveEligibleMembersForConversationAdminRoleUseCase
import com.wire.kalium.logic.feature.conversation.ObserveIsSelfUserMemberUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUsersTypingUseCase
import com.wire.kalium.logic.feature.conversation.PromoteAdminAndLeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.SetNotifiedAboutConversationUnderLegalHoldUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationCodeUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateChannelUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateRegularGroupUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedLocallyUseCase
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.CreateConversationFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.GetFavoriteFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.MoveConversationToFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveConversationsFromFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
@Suppress("TooManyFunctions")
class ConversationModule {

    @Provides
    fun provideConversationApi(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): KaliumConversationApi = coreLogic.getSessionScope(currentAccount).conversationApi

    @Provides
    fun provideObserveConversationListDetails(api: KaliumConversationApi): ObserveConversationListDetailsUseCase =
        api.conversationObserveConversationListDetails

    @Provides
    fun provideObserveConversationListDetailsWithEvents(
        api: KaliumConversationApi
    ): ObserveConversationListDetailsWithEventsUseCase =
        api.conversationObserveConversationListDetailsWithEvents

    @Provides
    fun provideObserveConversationUseCase(api: KaliumConversationApi): GetOneToOneConversationDetailsUseCase =
        api.conversationGetOneToOneConversation

    @Provides
    fun provideNotifyConversationIsOpenUseCase(api: KaliumConversationApi): NotifyConversationIsOpenUseCase =
        api.conversationNotifyConversationIsOpen

    @Provides
    fun provideDeleteTeamConversationUseCase(api: KaliumConversationApi): DeleteTeamConversationUseCase =
        api.conversationDeleteTeamConversation

    @Provides
    fun provideMarkConversationAsDeletedLocallyUseCase(api: KaliumConversationApi): MarkConversationAsDeletedLocallyUseCase =
        api.conversationMarkConversationAsDeletedLocallyUseCase

    @Provides
    fun provideObserveIsSelfConversationMemberUseCase(api: KaliumConversationApi): ObserveIsSelfUserMemberUseCase =
        api.conversationObserveIsSelfUserMemberUseCase

    @Provides
    fun provideObserveConversationInteractionAvailability(
        api: KaliumConversationApi
    ): ObserveConversationInteractionAvailabilityUseCase =
        api.conversationObserveConversationInteractionAvailabilityUseCase

    @Provides
    fun provideObserveConversationMembersUseCase(api: KaliumConversationApi): ObserveConversationMembersUseCase =
        api.conversationObserveConversationMembers

    @Provides
    fun provideMembersToMentionUseCase(api: KaliumConversationApi): MembersToMentionUseCase =
        api.conversationGetMembersToMention

    @Provides
    fun provideObserveUserListByIdUseCase(api: KaliumConversationApi): ObserveUserListByIdUseCase =
        api.conversationObserveUserListById

    @Provides
    fun providesJoinConversationViaCodeUseCase(api: KaliumConversationApi): JoinConversationViaCodeUseCase =
        api.conversationJoinConversationViaCode

    @Provides
    fun providesCanCreatePasswordProtectedLinksUseCase(api: KaliumConversationApi): CanCreatePasswordProtectedLinksUseCase =
        api.conversationCanCreatePasswordProtectedLinks

    @Provides
    fun provideRefreshConversationsWithoutMetadataUseCase(
        api: KaliumConversationApi
    ): RefreshConversationsWithoutMetadataUseCase =
        api.conversationRefreshConversationsWithoutMetadata

    @Provides
    fun provideGetConversationUnreadEventsCountUseCase(api: KaliumConversationApi): GetConversationUnreadEventsCountUseCase =
        api.conversationGetConversationUnreadEventsCountUseCase

    @Provides
    fun provideUpdateMessageTimerUseCase(api: KaliumConversationApi): UpdateMessageTimerUseCase =
        api.conversationUpdateMessageTimer

    @Provides
    fun provideRenameConversation(api: KaliumConversationApi): RenameConversationUseCase =
        api.conversationRenameConversation

    @Provides
    fun provideUpdateConversationReadDateUseCase(api: KaliumConversationApi): UpdateConversationReadDateUseCase =
        api.conversationUpdateConversationReadDateUseCase

    @Provides
    fun provideMarkConversationAsReadLocallyUseCase(api: KaliumConversationApi): MarkConversationAsReadLocallyUseCase =
        api.conversationMarkConversationAsReadLocally

    @Provides
    fun provideUpdateConversationAccessUseCase(api: KaliumConversationApi): UpdateConversationAccessRoleUseCase =
        api.conversationUpdateConversationAccess

    @Provides
    fun provideLeaveConversationUseCase(api: KaliumConversationApi): LeaveConversationUseCase =
        api.conversationLeaveConversation

    @Provides
    fun providePromoteAdminAndLeaveConversationUseCase(api: KaliumConversationApi): PromoteAdminAndLeaveConversationUseCase =
        api.conversationPromoteAdminAndLeaveConversation

    @Provides
    fun provideCheckConversationLeaveConditionsUseCase(api: KaliumConversationApi): CheckConversationLeaveConditionsUseCase =
        api.conversationCheckConversationLeaveConditions

    @Provides
    fun provideObserveEligibleMembersForConversationAdminRoleUseCase(
        api: KaliumConversationApi
    ): ObserveEligibleMembersForConversationAdminRoleUseCase =
        api.conversationObserveEligibleMembersForConversationAdminRole

    @Provides
    fun provideUpdateConversationMutedStatusUseCase(api: KaliumConversationApi): UpdateConversationMutedStatusUseCase =
        api.conversationUpdateConversationMutedStatus

    @Provides
    fun provideUpdateConversationReceiptModeUseCase(api: KaliumConversationApi): UpdateConversationReceiptModeUseCase =
        api.conversationUpdateConversationReceiptMode

    @Provides
    fun provideAddServiceToConversationUseCase(api: KaliumConversationApi): AddServiceToConversationUseCase =
        api.conversationAddServiceToConversationUseCase

    @Provides
    fun provideRemoveMemberFromConversationUseCase(api: KaliumConversationApi): RemoveMemberFromConversationUseCase =
        api.conversationRemoveMemberFromConversation

    @Provides
    fun provideCreateRegularGroupUseCase(api: KaliumConversationApi): CreateRegularGroupUseCase =
        api.conversationCreateRegularGroup

    @Provides
    fun provideCreateChannelUseCase(api: KaliumConversationApi): CreateChannelUseCase =
        api.conversationCreateChannel

    @Provides
    fun provideAddMemberToConversationUseCase(api: KaliumConversationApi): AddMemberToConversationUseCase =
        api.conversationAddMemberToConversationUseCase

    @Provides
    fun provideGetOrCreateOneToOneConversationUseCase(api: KaliumConversationApi): GetOrCreateOneToOneConversationUseCase =
        api.conversationGetOrCreateOneToOneConversationUseCase

    @Provides
    fun provideGenerateGuestRoomLinkUseCase(api: KaliumConversationApi): GenerateGuestRoomLinkUseCase =
        api.conversationGenerateGuestRoomLink

    @Provides
    fun provideRevokeGuestRoomLinkUseCase(api: KaliumConversationApi): RevokeGuestRoomLinkUseCase =
        api.conversationRevokeGuestRoomLink

    @Provides
    fun provideObserveGuestRoomLinkUseCase(api: KaliumConversationApi): ObserveGuestRoomLinkUseCase =
        api.conversationObserveGuestRoomLink

    @Provides
    fun provideClearConversationContentUseCase(api: KaliumConversationApi): ClearConversationContentUseCase =
        api.conversationClearConversationContent

    @Provides
    fun provideUpdateConversationArchivedStatusUseCase(api: KaliumConversationApi): UpdateConversationArchivedStatusUseCase =
        api.conversationUpdateConversationArchivedStatus

    @Provides
    fun provideUpdateConversationMemberRoleUseCase(api: KaliumConversationApi): UpdateConversationMemberRoleUseCase =
        api.conversationUpdateConversationMemberRole

    @Provides
    fun provideObserveArchivedUnreadConversationsCountUseCase(
        api: KaliumConversationApi
    ): ObserveArchivedUnreadConversationsCountUseCase = api.conversationObserveArchivedUnreadConversationsCount

    @Provides
    fun provideObserveUsersTypingUseCase(api: KaliumConversationApi): ObserveUsersTypingUseCase =
        api.conversationObserveUsersTyping

    @Provides
    fun provideSendTypingEventUseCase(api: KaliumConversationApi): SendTypingEventUseCase = api.conversationSendTypingEvent

    @Provides
    fun provideClearTypingEventsUseCase(api: KaliumConversationApi): ClearUsersTypingEventsUseCase =
        api.conversationClearUsersTypingEvents

    @Provides
    fun provideSetUserInformedAboutVerificationBeforeMessagingUseCase(
        api: KaliumConversationApi
    ): SetUserInformedAboutVerificationUseCase =
        api.conversationSetUserInformedAboutVerificationBeforeMessagingUseCase

    @Provides
    fun provideObserveInformAboutVerificationBeforeMessagingFlagUseCase(
        api: KaliumConversationApi
    ): ObserveDegradedConversationNotifiedUseCase =
        api.conversationObserveInformAboutVerificationBeforeMessagingFlagUseCase

    @Provides
    fun provideSetUserNotifiedAboutConversationUnderLegalHoldUseCase(
        api: KaliumConversationApi,
    ): SetNotifiedAboutConversationUnderLegalHoldUseCase = api.conversationSetNotifiedAboutConversationUnderLegalHold

    @Provides
    fun provideObserveLegalHoldWithChangeNotifiedForConversationUseCase(
        api: KaliumConversationApi,
    ): ObserveConversationUnderLegalHoldNotifiedUseCase = api.conversationObserveConversationUnderLegalHoldNotified

    @Provides
    fun provideSyncConversationCodeUseCase(api: KaliumConversationApi): SyncConversationCodeUseCase =
        api.conversationSyncConversationCode

    @Provides
    fun provideIsOneToOneConversationCreatedUseCase(api: KaliumConversationApi): IsOneToOneConversationCreatedUseCase =
        api.conversationIsOneToOneConversationCreatedUseCase

    @Provides
    fun provideCheckOneToOneConversationIsReadyUseCase(
        api: KaliumConversationApi
    ): CheckOneToOneConversationIsReadyUseCase = api.conversationCheckOneToOneConversationIsReadyUseCase

    @Provides
    fun provideGetConversationProtocolInfoUseCase(api: KaliumConversationApi): GetConversationProtocolInfoUseCase =
        api.conversationGetConversationProtocolInfo

    @Provides
    fun provideGetPaginatedFlowOfConversationDetailsWithMembersBySearchQueryUseCase(
        api: KaliumConversationApi
    ): GetPaginatedFlowOfConversationDetailsWithMembersBySearchQueryUseCase =
        api.conversationGetPaginatedFlowOfConversationDetailsWithMembersBySearchQuery

    @Provides
    fun provideGetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase(
        api: KaliumConversationApi
    ): GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase =
        api.conversationGetPaginatedFlowOfConversationDetailsWithEventsBySearchQuery

    @Provides
    fun provideObserveConversationsFromFolderUseCase(api: KaliumConversationApi): ObserveConversationsFromFolderUseCase =
        api.conversationObserveConversationsFromFolder

    @Provides
    fun provideGetFavoriteFolderUseCase(api: KaliumConversationApi): GetFavoriteFolderUseCase =
        api.conversationGetFavoriteFolder

    @Provides
    fun provideAddConversationToFavoritesUseCase(api: KaliumConversationApi): AddConversationToFavoritesUseCase =
        api.conversationAddConversationToFavorites

    @Provides
    fun provideRemoveConversationFromFavoritesUseCase(api: KaliumConversationApi): RemoveConversationFromFavoritesUseCase =
        api.conversationRemoveConversationFromFavorites

    @Provides
    fun provideObserveUserFoldersUseCase(api: KaliumConversationApi): ObserveUserFoldersUseCase =
        api.conversationObserveUserFolders

    @Provides
    fun provideMoveConversationToFolderUseCase(api: KaliumConversationApi): MoveConversationToFolderUseCase =
        api.conversationMoveConversationToFolder

    @Provides
    fun provideRemoveConversationFromFolderUseCase(api: KaliumConversationApi): RemoveConversationFromFolderUseCase =
        api.conversationRemoveConversationFromFolder

    @Provides
    fun provideCreateConversationFolderUseCase(api: KaliumConversationApi): CreateConversationFolderUseCase =
        api.conversationCreateConversationFolder

    @Provides
    fun provideResetMlsConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ResetMLSConversationUseCase = coreLogic.getSessionScope(currentAccount).resetMlsConversation

    @Provides
    fun provideMigrateConversationToMLSUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MigrateConversationToMLSUseCase = coreLogic.getSessionScope(currentAccount).migrateConversationToMLS

    @Provides
    fun provideFetchConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FetchConversationUseCase = coreLogic.getSessionScope(currentAccount).fetchConversationUseCase

    @Provides
    fun provideChangeAccessForAppsInConversationUseCase(
        api: KaliumConversationApi
    ): ChangeAccessForAppsInConversationUseCase =
        api.conversationChangeAccessForAppsInConversation

    @Provides
    fun provideIsSelfUserViewerOnConversationUseCase(
        api: KaliumConversationApi
    ): IsSelfUserViewerOnConversationUseCase = api.conversationIsSelfUserViewerOnConversation
}
