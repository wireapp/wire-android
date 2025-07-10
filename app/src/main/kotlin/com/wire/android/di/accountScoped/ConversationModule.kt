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
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.NotifyConversationIsOpenUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
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
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateChannelUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateRegularGroupUseCase
import com.wire.kalium.logic.feature.conversation.getPaginatedFlowOfConversationDetailsWithEventsBySearchQuery
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions")
class ConversationModule {

    @Provides
    @ViewModelScoped
    fun provideConversationScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ConversationScope = coreLogic.getSessionScope(currentAccount).conversations

    @ViewModelScoped
    @Provides
    fun provideObserveConversationListDetails(conversationScope: ConversationScope): ObserveConversationListDetailsUseCase =
        conversationScope.observeConversationListDetails

    @ViewModelScoped
    @Provides
    fun provideObserveConversationListDetailsWithEvents(conversationScope: ConversationScope) =
        conversationScope.observeConversationListDetailsWithEvents

    @ViewModelScoped
    @Provides
    fun provideObserveConversationUseCase(conversationScope: ConversationScope): GetOneToOneConversationDetailsUseCase =
        conversationScope.getOneToOneConversation

    @ViewModelScoped
    @Provides
    fun provideObserveConversationDetailsUseCase(conversationScope: ConversationScope): ObserveConversationDetailsUseCase =
        conversationScope.observeConversationDetails

    @ViewModelScoped
    @Provides
    fun provideObserveConversationDetailsWithEventsUseCase(
        conversationScope: ConversationScope
    ): ObserveConversationDetailsWithEventsUseCase = conversationScope.observeConversationDetailsWithEvents

    @ViewModelScoped
    @Provides
    fun provideNotifyConversationIsOpenUseCase(conversationScope: ConversationScope): NotifyConversationIsOpenUseCase =
        conversationScope.notifyConversationIsOpen

    @ViewModelScoped
    @Provides
    fun provideDeleteTeamConversationUseCase(conversationScope: ConversationScope): DeleteTeamConversationUseCase =
        conversationScope.deleteTeamConversation

    @ViewModelScoped
    @Provides
    fun provideObserveIsSelfConversationMemberUseCase(conversationScope: ConversationScope): ObserveIsSelfUserMemberUseCase =
        conversationScope.observeIsSelfUserMemberUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveConversationInteractionAvailability(
        conversationScope: ConversationScope
    ): ObserveConversationInteractionAvailabilityUseCase =
        conversationScope.observeConversationInteractionAvailabilityUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveConversationMembersUseCase(conversationScope: ConversationScope): ObserveConversationMembersUseCase =
        conversationScope.observeConversationMembers

    @ViewModelScoped
    @Provides
    fun provideMembersToMentionUseCase(conversationScope: ConversationScope) =
        conversationScope.getMembersToMention

    @ViewModelScoped
    @Provides
    fun provideObserveUserListByIdUseCase(conversationScope: ConversationScope): ObserveUserListByIdUseCase =
        conversationScope.observeUserListById

    @ViewModelScoped
    @Provides
    fun providesJoinConversationViaCodeUseCase(conversationScope: ConversationScope): JoinConversationViaCodeUseCase =
        conversationScope.joinConversationViaCode

    @ViewModelScoped
    @Provides
    fun providesCanCreatePasswordProtectedLinksUseCase(conversationScope: ConversationScope): CanCreatePasswordProtectedLinksUseCase =
        conversationScope.canCreatePasswordProtectedLinks

    @ViewModelScoped
    @Provides
    fun provideRefreshConversationsWithoutMetadataUseCase(
        conversationScope: ConversationScope
    ): RefreshConversationsWithoutMetadataUseCase =
        conversationScope.refreshConversationsWithoutMetadata

    @ViewModelScoped
    @Provides
    fun provideGetConversationUnreadEventsCountUseCase(conversationScope: ConversationScope): GetConversationUnreadEventsCountUseCase =
        conversationScope.getConversationUnreadEventsCountUseCase

    @ViewModelScoped
    @Provides
    fun provideUpdateMessageTimerUseCase(conversationScope: ConversationScope): UpdateMessageTimerUseCase =
        conversationScope.updateMessageTimer

    @ViewModelScoped
    @Provides
    fun provideRenameConversation(conversationScope: ConversationScope): RenameConversationUseCase =
        conversationScope.renameConversation

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationReadDateUseCase(conversationScope: ConversationScope): UpdateConversationReadDateUseCase =
        conversationScope.updateConversationReadDateUseCase

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationAccessUseCase(conversationScope: ConversationScope): UpdateConversationAccessRoleUseCase =
        conversationScope.updateConversationAccess

    @ViewModelScoped
    @Provides
    fun provideLeaveConversationUseCase(conversationScope: ConversationScope): LeaveConversationUseCase =
        conversationScope.leaveConversation

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationMutedStatusUseCase(conversationScope: ConversationScope): UpdateConversationMutedStatusUseCase =
        conversationScope.updateConversationMutedStatus

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationReceiptModeUseCase(conversationScope: ConversationScope): UpdateConversationReceiptModeUseCase =
        conversationScope.updateConversationReceiptMode

    @ViewModelScoped
    @Provides
    fun provideAddServiceToConversationUseCase(conversationScope: ConversationScope): AddServiceToConversationUseCase =
        conversationScope.addServiceToConversationUseCase

    @ViewModelScoped
    @Provides
    fun provideRemoveMemberFromConversationUseCase(conversationScope: ConversationScope): RemoveMemberFromConversationUseCase =
        conversationScope.removeMemberFromConversation

    @ViewModelScoped
    @Provides
    fun provideCreateRegularGroupUseCase(conversationScope: ConversationScope): CreateRegularGroupUseCase =
        conversationScope.createRegularGroup

    @ViewModelScoped
    @Provides
    fun provideCreateChannelUseCase(conversationScope: ConversationScope): CreateChannelUseCase =
        conversationScope.createChannel

    @ViewModelScoped
    @Provides
    fun provideAddMemberToConversationUseCase(conversationScope: ConversationScope): AddMemberToConversationUseCase =
        conversationScope.addMemberToConversationUseCase

    @ViewModelScoped
    @Provides
    fun provideGetOrCreateOneToOneConversationUseCase(conversationScope: ConversationScope): GetOrCreateOneToOneConversationUseCase =
        conversationScope.getOrCreateOneToOneConversationUseCase

    @ViewModelScoped
    @Provides
    fun provideGenerateGuestRoomLinkUseCase(conversationScope: ConversationScope): GenerateGuestRoomLinkUseCase =
        conversationScope.generateGuestRoomLink

    @ViewModelScoped
    @Provides
    fun provideRevokeGuestRoomLinkUseCase(conversationScope: ConversationScope): RevokeGuestRoomLinkUseCase =
        conversationScope.revokeGuestRoomLink

    @ViewModelScoped
    @Provides
    fun provideObserveGuestRoomLinkUseCase(conversationScope: ConversationScope): ObserveGuestRoomLinkUseCase =
        conversationScope.observeGuestRoomLink

    @ViewModelScoped
    @Provides
    fun provideClearConversationContentUseCase(conversationScope: ConversationScope): ClearConversationContentUseCase =
        conversationScope.clearConversationContent

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationArchivedStatusUseCase(conversationScope: ConversationScope): UpdateConversationArchivedStatusUseCase =
        conversationScope.updateConversationArchivedStatus

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationMemberRoleUseCase(conversationScope: ConversationScope): UpdateConversationMemberRoleUseCase =
        conversationScope.updateConversationMemberRole

    @ViewModelScoped
    @Provides
    fun provideObserveArchivedUnreadConversationsCountUseCase(
        conversationScope: ConversationScope
    ): ObserveArchivedUnreadConversationsCountUseCase = conversationScope.observeArchivedUnreadConversationsCount

    @ViewModelScoped
    @Provides
    fun provideObserveUsersTypingUseCase(conversationScope: ConversationScope): ObserveUsersTypingUseCase =
        conversationScope.observeUsersTyping

    @ViewModelScoped
    @Provides
    fun provideSendTypingEventUseCase(conversationScope: ConversationScope): SendTypingEventUseCase = conversationScope.sendTypingEvent

    @ViewModelScoped
    @Provides
    fun provideClearTypingEventsUseCase(conversationScope: ConversationScope): ClearUsersTypingEventsUseCase =
        conversationScope.clearUsersTypingEvents

    @ViewModelScoped
    @Provides
    fun provideSetUserInformedAboutVerificationBeforeMessagingUseCase(
        conversationScope: ConversationScope
    ): SetUserInformedAboutVerificationUseCase =
        conversationScope.setUserInformedAboutVerificationBeforeMessagingUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveInformAboutVerificationBeforeMessagingFlagUseCase(
        conversationScope: ConversationScope
    ): ObserveDegradedConversationNotifiedUseCase =
        conversationScope.observeInformAboutVerificationBeforeMessagingFlagUseCase

    @ViewModelScoped
    @Provides
    fun provideSetUserNotifiedAboutConversationUnderLegalHoldUseCase(
        conversationScope: ConversationScope,
    ): SetNotifiedAboutConversationUnderLegalHoldUseCase = conversationScope.setNotifiedAboutConversationUnderLegalHold

    @ViewModelScoped
    @Provides
    fun provideObserveLegalHoldWithChangeNotifiedForConversationUseCase(
        conversationScope: ConversationScope,
    ): ObserveConversationUnderLegalHoldNotifiedUseCase = conversationScope.observeConversationUnderLegalHoldNotified

    @ViewModelScoped
    @Provides
    fun provideSyncConversationCodeUseCase(conversationScope: ConversationScope): SyncConversationCodeUseCase =
        conversationScope.syncConversationCode

    @ViewModelScoped
    @Provides
    fun provideIsOneToOneConversationCreatedUseCase(conversationScope: ConversationScope): IsOneToOneConversationCreatedUseCase =
        conversationScope.isOneToOneConversationCreatedUseCase

    @ViewModelScoped
    @Provides
    fun provideGetConversationProtocolInfoUseCase(conversationScope: ConversationScope): GetConversationProtocolInfoUseCase =
        conversationScope.getConversationProtocolInfo

    @ViewModelScoped
    @Provides
    fun provideGetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase(conversationScope: ConversationScope) =
        conversationScope.getPaginatedFlowOfConversationDetailsWithEventsBySearchQuery

    @ViewModelScoped
    @Provides
    fun provideObserveConversationsFromFolderUseCase(conversationScope: ConversationScope) =
        conversationScope.observeConversationsFromFolder

    @ViewModelScoped
    @Provides
    fun provideGetFavoriteFolderUseCase(conversationScope: ConversationScope) =
        conversationScope.getFavoriteFolder

    @ViewModelScoped
    @Provides
    fun provideAddConversationToFavoritesUseCase(conversationScope: ConversationScope) =
        conversationScope.addConversationToFavorites

    @ViewModelScoped
    @Provides
    fun provideRemoveConversationFromFavoritesUseCase(conversationScope: ConversationScope) =
        conversationScope.removeConversationFromFavorites

    @ViewModelScoped
    @Provides
    fun provideObserveUserFoldersUseCase(conversationScope: ConversationScope) =
        conversationScope.observeUserFolders

    @ViewModelScoped
    @Provides
    fun provideMoveConversationToFolderUseCase(conversationScope: ConversationScope) =
        conversationScope.moveConversationToFolder

    @ViewModelScoped
    @Provides
    fun provideRemoveConversationFromFolderUseCase(conversationScope: ConversationScope) =
        conversationScope.removeConversationFromFolder

    @ViewModelScoped
    @Provides
    fun provideCreateConversationFolderUseCase(conversationScope: ConversationScope) =
        conversationScope.createConversationFolder
}
