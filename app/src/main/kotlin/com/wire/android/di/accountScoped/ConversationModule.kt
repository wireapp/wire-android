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
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ClearUsersTypingEventsUseCase
import com.wire.kalium.logic.feature.conversation.ConversationScope
import com.wire.kalium.logic.feature.conversation.GetConversationProtocolInfoUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationLeaveConditionsUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveEligibleMembersForConversationAdminRoleUseCase
import com.wire.kalium.logic.feature.conversation.PromoteAdminAndLeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.NotifyConversationIsOpenUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveIsSelfUserMemberUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUsersTypingUseCase
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
import com.wire.kalium.logic.feature.conversation.MarkConversationAsReadLocallyUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateChannelUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateRegularGroupUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedLocallyUseCase
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.CreateConversationFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.GetFavoriteFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.MoveConversationToFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveConversationsFromFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import com.wire.kalium.logic.feature.conversation.getPaginatedFlowOfConversationDetailsWithEventsBySearchQuery
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import dagger.Module
import dagger.Provides

@Module
@Suppress("TooManyFunctions")
class ConversationModule {

    @Provides
    fun provideConversationScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ConversationScope = coreLogic.getSessionScope(currentAccount).conversations

    @Provides
    fun provideObserveConversationListDetails(conversationScope: ConversationScope): ObserveConversationListDetailsUseCase =
        conversationScope.observeConversationListDetails

    @Provides
    fun provideObserveConversationListDetailsWithEvents(
        conversationScope: ConversationScope
    ): ObserveConversationListDetailsWithEventsUseCase =
        conversationScope.observeConversationListDetailsWithEvents

    @Provides
    fun provideObserveConversationUseCase(conversationScope: ConversationScope): GetOneToOneConversationDetailsUseCase =
        conversationScope.getOneToOneConversation

    @Provides
    fun provideObserveConversationDetailsUseCase(conversationScope: ConversationScope): ObserveConversationDetailsUseCase =
        conversationScope.observeConversationDetails

    @Provides
    fun provideNotifyConversationIsOpenUseCase(conversationScope: ConversationScope): NotifyConversationIsOpenUseCase =
        conversationScope.notifyConversationIsOpen

    @Provides
    fun provideDeleteTeamConversationUseCase(conversationScope: ConversationScope): DeleteTeamConversationUseCase =
        conversationScope.deleteTeamConversation

    @Provides
    fun provideMarkConversationAsDeletedLocallyUseCase(conversationScope: ConversationScope): MarkConversationAsDeletedLocallyUseCase =
        conversationScope.markConversationAsDeletedLocallyUseCase

    @Provides
    fun provideObserveIsSelfConversationMemberUseCase(conversationScope: ConversationScope): ObserveIsSelfUserMemberUseCase =
        conversationScope.observeIsSelfUserMemberUseCase

    @Provides
    fun provideObserveConversationInteractionAvailability(
        conversationScope: ConversationScope
    ): ObserveConversationInteractionAvailabilityUseCase =
        conversationScope.observeConversationInteractionAvailabilityUseCase

    @Provides
    fun provideObserveConversationMembersUseCase(conversationScope: ConversationScope): ObserveConversationMembersUseCase =
        conversationScope.observeConversationMembers

    @Provides
    fun provideMembersToMentionUseCase(conversationScope: ConversationScope): MembersToMentionUseCase =
        conversationScope.getMembersToMention

    @Provides
    fun provideObserveUserListByIdUseCase(conversationScope: ConversationScope): ObserveUserListByIdUseCase =
        conversationScope.observeUserListById

    @Provides
    fun providesJoinConversationViaCodeUseCase(conversationScope: ConversationScope): JoinConversationViaCodeUseCase =
        conversationScope.joinConversationViaCode

    @Provides
    fun providesCanCreatePasswordProtectedLinksUseCase(conversationScope: ConversationScope): CanCreatePasswordProtectedLinksUseCase =
        conversationScope.canCreatePasswordProtectedLinks

    @Provides
    fun provideRefreshConversationsWithoutMetadataUseCase(
        conversationScope: ConversationScope
    ): RefreshConversationsWithoutMetadataUseCase =
        conversationScope.refreshConversationsWithoutMetadata

    @Provides
    fun provideGetConversationUnreadEventsCountUseCase(conversationScope: ConversationScope): GetConversationUnreadEventsCountUseCase =
        conversationScope.getConversationUnreadEventsCountUseCase

    @Provides
    fun provideUpdateMessageTimerUseCase(conversationScope: ConversationScope): UpdateMessageTimerUseCase =
        conversationScope.updateMessageTimer

    @Provides
    fun provideRenameConversation(conversationScope: ConversationScope): RenameConversationUseCase =
        conversationScope.renameConversation

    @Provides
    fun provideUpdateConversationReadDateUseCase(conversationScope: ConversationScope): UpdateConversationReadDateUseCase =
        conversationScope.updateConversationReadDateUseCase

    @Provides
    fun provideMarkConversationAsReadLocallyUseCase(conversationScope: ConversationScope): MarkConversationAsReadLocallyUseCase =
        conversationScope.markConversationAsReadLocally

    @Provides
    fun provideUpdateConversationAccessUseCase(conversationScope: ConversationScope): UpdateConversationAccessRoleUseCase =
        conversationScope.updateConversationAccess

    @Provides
    fun provideLeaveConversationUseCase(conversationScope: ConversationScope): LeaveConversationUseCase =
        conversationScope.leaveConversation

    @Provides
    fun providePromoteAdminAndLeaveConversationUseCase(conversationScope: ConversationScope): PromoteAdminAndLeaveConversationUseCase =
        conversationScope.promoteAdminAndLeaveConversation

    @Provides
    fun provideCheckConversationLeaveConditionsUseCase(conversationScope: ConversationScope): CheckConversationLeaveConditionsUseCase =
        conversationScope.checkConversationLeaveConditions

    @Provides
    fun provideObserveEligibleMembersForConversationAdminRoleUseCase(
        conversationScope: ConversationScope
    ): ObserveEligibleMembersForConversationAdminRoleUseCase =
        conversationScope.observeEligibleMembersForConversationAdminRole

    @Provides
    fun provideUpdateConversationMutedStatusUseCase(conversationScope: ConversationScope): UpdateConversationMutedStatusUseCase =
        conversationScope.updateConversationMutedStatus

    @Provides
    fun provideUpdateConversationReceiptModeUseCase(conversationScope: ConversationScope): UpdateConversationReceiptModeUseCase =
        conversationScope.updateConversationReceiptMode

    @Provides
    fun provideAddServiceToConversationUseCase(conversationScope: ConversationScope): AddServiceToConversationUseCase =
        conversationScope.addServiceToConversationUseCase

    @Provides
    fun provideRemoveMemberFromConversationUseCase(conversationScope: ConversationScope): RemoveMemberFromConversationUseCase =
        conversationScope.removeMemberFromConversation

    @Provides
    fun provideCreateRegularGroupUseCase(conversationScope: ConversationScope): CreateRegularGroupUseCase =
        conversationScope.createRegularGroup

    @Provides
    fun provideCreateChannelUseCase(conversationScope: ConversationScope): CreateChannelUseCase =
        conversationScope.createChannel

    @Provides
    fun provideAddMemberToConversationUseCase(conversationScope: ConversationScope): AddMemberToConversationUseCase =
        conversationScope.addMemberToConversationUseCase

    @Provides
    fun provideGetOrCreateOneToOneConversationUseCase(conversationScope: ConversationScope): GetOrCreateOneToOneConversationUseCase =
        conversationScope.getOrCreateOneToOneConversationUseCase

    @Provides
    fun provideGenerateGuestRoomLinkUseCase(conversationScope: ConversationScope): GenerateGuestRoomLinkUseCase =
        conversationScope.generateGuestRoomLink

    @Provides
    fun provideRevokeGuestRoomLinkUseCase(conversationScope: ConversationScope): RevokeGuestRoomLinkUseCase =
        conversationScope.revokeGuestRoomLink

    @Provides
    fun provideObserveGuestRoomLinkUseCase(conversationScope: ConversationScope): ObserveGuestRoomLinkUseCase =
        conversationScope.observeGuestRoomLink

    @Provides
    fun provideClearConversationContentUseCase(conversationScope: ConversationScope): ClearConversationContentUseCase =
        conversationScope.clearConversationContent

    @Provides
    fun provideUpdateConversationArchivedStatusUseCase(conversationScope: ConversationScope): UpdateConversationArchivedStatusUseCase =
        conversationScope.updateConversationArchivedStatus

    @Provides
    fun provideUpdateConversationMemberRoleUseCase(conversationScope: ConversationScope): UpdateConversationMemberRoleUseCase =
        conversationScope.updateConversationMemberRole

    @Provides
    fun provideObserveArchivedUnreadConversationsCountUseCase(
        conversationScope: ConversationScope
    ): ObserveArchivedUnreadConversationsCountUseCase = conversationScope.observeArchivedUnreadConversationsCount

    @Provides
    fun provideObserveUsersTypingUseCase(conversationScope: ConversationScope): ObserveUsersTypingUseCase =
        conversationScope.observeUsersTyping

    @Provides
    fun provideSendTypingEventUseCase(conversationScope: ConversationScope): SendTypingEventUseCase = conversationScope.sendTypingEvent

    @Provides
    fun provideClearTypingEventsUseCase(conversationScope: ConversationScope): ClearUsersTypingEventsUseCase =
        conversationScope.clearUsersTypingEvents

    @Provides
    fun provideSetUserInformedAboutVerificationBeforeMessagingUseCase(
        conversationScope: ConversationScope
    ): SetUserInformedAboutVerificationUseCase =
        conversationScope.setUserInformedAboutVerificationBeforeMessagingUseCase

    @Provides
    fun provideObserveInformAboutVerificationBeforeMessagingFlagUseCase(
        conversationScope: ConversationScope
    ): ObserveDegradedConversationNotifiedUseCase =
        conversationScope.observeInformAboutVerificationBeforeMessagingFlagUseCase

    @Provides
    fun provideSetUserNotifiedAboutConversationUnderLegalHoldUseCase(
        conversationScope: ConversationScope,
    ): SetNotifiedAboutConversationUnderLegalHoldUseCase = conversationScope.setNotifiedAboutConversationUnderLegalHold

    @Provides
    fun provideObserveLegalHoldWithChangeNotifiedForConversationUseCase(
        conversationScope: ConversationScope,
    ): ObserveConversationUnderLegalHoldNotifiedUseCase = conversationScope.observeConversationUnderLegalHoldNotified

    @Provides
    fun provideSyncConversationCodeUseCase(conversationScope: ConversationScope): SyncConversationCodeUseCase =
        conversationScope.syncConversationCode

    @Provides
    fun provideIsOneToOneConversationCreatedUseCase(conversationScope: ConversationScope): IsOneToOneConversationCreatedUseCase =
        conversationScope.isOneToOneConversationCreatedUseCase

    @Provides
    fun provideGetConversationProtocolInfoUseCase(conversationScope: ConversationScope): GetConversationProtocolInfoUseCase =
        conversationScope.getConversationProtocolInfo

    @Provides
    fun provideGetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase(
        conversationScope: ConversationScope
    ): GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase =
        conversationScope.getPaginatedFlowOfConversationDetailsWithEventsBySearchQuery

    @Provides
    fun provideObserveConversationsFromFolderUseCase(conversationScope: ConversationScope): ObserveConversationsFromFolderUseCase =
        conversationScope.observeConversationsFromFolder

    @Provides
    fun provideGetFavoriteFolderUseCase(conversationScope: ConversationScope): GetFavoriteFolderUseCase =
        conversationScope.getFavoriteFolder

    @Provides
    fun provideAddConversationToFavoritesUseCase(conversationScope: ConversationScope): AddConversationToFavoritesUseCase =
        conversationScope.addConversationToFavorites

    @Provides
    fun provideRemoveConversationFromFavoritesUseCase(
        conversationScope: ConversationScope
    ): RemoveConversationFromFavoritesUseCase =
        conversationScope.removeConversationFromFavorites

    @Provides
    fun provideObserveUserFoldersUseCase(conversationScope: ConversationScope): ObserveUserFoldersUseCase =
        conversationScope.observeUserFolders

    @Provides
    fun provideMoveConversationToFolderUseCase(conversationScope: ConversationScope): MoveConversationToFolderUseCase =
        conversationScope.moveConversationToFolder

    @Provides
    fun provideRemoveConversationFromFolderUseCase(conversationScope: ConversationScope): RemoveConversationFromFolderUseCase =
        conversationScope.removeConversationFromFolder

    @Provides
    fun provideCreateConversationFolderUseCase(conversationScope: ConversationScope): CreateConversationFolderUseCase =
        conversationScope.createConversationFolder

    @Provides
    fun provideResetMlsConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ResetMLSConversationUseCase = coreLogic.getSessionScope(currentAccount).resetMlsConversation

    @Provides
    fun provideFetchConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FetchConversationUseCase = coreLogic.getSessionScope(currentAccount).fetchConversationUseCase

    @Provides
    fun provideChangeAccessForAppsInConversationUseCase(
        conversationScope: ConversationScope
    ): ChangeAccessForAppsInConversationUseCase =
        conversationScope.changeAccessForAppsInConversation
}
