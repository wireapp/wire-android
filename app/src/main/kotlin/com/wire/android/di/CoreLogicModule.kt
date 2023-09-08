/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.di

import android.content.Context
import androidx.work.WorkManager
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.util.ImageUtil
import com.wire.android.util.UserAgentProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.FederatedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallOnConversationChangeUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveOtherUserSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.GetNotificationsUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageReactionsUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageReceiptsUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.message.getPaginatedFlowOfMessagesByConversation
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.GetKnownUserUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchPublicUsersUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveTeamSettingsSelfDeletingStatusUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.server.ServerConfigForAccountUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.MarkFileSharingChangeAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.MarkSelfDeletionStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.network.NetworkStateObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KaliumCoreLogic

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentSessionFlowService

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentAccount

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoSession

@Module
@InstallIn(SingletonComponent::class)
class CoreLogicModule {

    @KaliumCoreLogic
    @Singleton
    @Provides
    fun provideCoreLogic(
        @ApplicationContext context: Context,
        kaliumConfigs: KaliumConfigs,
        userAgentProvider: UserAgentProvider
    ): CoreLogic {
        val rootPath = context.getDir("accounts", Context.MODE_PRIVATE).path

        return CoreLogic(
            userAgent = userAgentProvider.defaultUserAgent,
            appContext = context,
            rootPath = rootPath,
            kaliumConfigs = kaliumConfigs
        )
    }

    @Singleton
    @Provides
    fun provideNetworkStateObserver(@KaliumCoreLogic coreLogic: CoreLogic): NetworkStateObserver =
        coreLogic.networkStateObserver

    @Provides
    fun provideCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSession

    @Provides
    fun deleteSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().deleteSession

    @Provides
    fun provideUpdateCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): UpdateCurrentSessionUseCase =
        coreLogic.getGlobalScope().session.updateCurrentSession

    @Provides
    fun provideGetAllSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): GetSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessions

    @Provides
    fun provideServerConfigForAccountUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ServerConfigForAccountUseCase =
        coreLogic.getGlobalScope().serverConfigForAccounts

    @NoSession
    @Singleton
    @Provides
    fun provideNoSessionQualifiedIdMapper(): QualifiedIdMapper = QualifiedIdMapperImpl(null)

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext applicationContext: Context) = WorkManager.getInstance(applicationContext)
}

@Module
@InstallIn(ViewModelComponent::class)
class SessionModule {
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    @CurrentAccount
    @ViewModelScoped
    @Provides
    fun provideCurrentSession(@KaliumCoreLogic coreLogic: CoreLogic): UserId {
        return runBlocking {
            return@runBlocking when (val result = coreLogic.getGlobalScope().session.currentSession.invoke()) {
                is CurrentSessionResult.Success -> result.accountInfo.userId
                else -> {
                    throw IllegalStateException("no current session was found")
                }
            }
        }
    }

    @ViewModelScoped
    @Provides
    fun provideCurrentAccountUserDataStore(@CurrentAccount currentAccount: UserId, userDataStoreProvider: UserDataStoreProvider) =
        userDataStoreProvider.getOrCreate(currentAccount)
}

@Module
@InstallIn(ViewModelComponent::class)
class ConnectionModule {
    @ViewModelScoped
    @Provides
    fun provideSendConnectionRequestUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.sendConnectionRequest

    @ViewModelScoped
    @Provides
    fun provideCancelConnectionRequestUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.cancelConnectionRequest

    @ViewModelScoped
    @Provides
    fun provideIgnoreConnectionRequestUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.ignoreConnectionRequest

    @ViewModelScoped
    @Provides
    fun provideAcceptConnectionRequestUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.acceptConnectionRequest
}

@Module
@InstallIn(ServiceComponent::class)
class ServiceModule {
    @ServiceScoped
    @Provides
    @CurrentSessionFlowService
    fun provideCurrentSessionFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSessionFlow
}

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions", "LargeClass")
class UseCaseModule {

    @ViewModelScoped
    @Provides
    fun provideObserveSyncStateUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeSyncState

    @ViewModelScoped
    @Provides
    fun provideLogoutUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): LogoutUseCase =
        coreLogic.getSessionScope(currentAccount).logout

    @ViewModelScoped
    @Provides
    fun provideValidateEmailUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().validateEmailUseCase

    @ViewModelScoped
    @Provides
    fun provideValidatePasswordUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().validatePasswordUseCase

    @ViewModelScoped
    @Provides
    fun provideValidateUserHandleUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().validateUserHandleUseCase

    @ViewModelScoped
    @Provides
    fun provideSetUserHandleUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.setUserHandle

    @ViewModelScoped
    @Provides
    fun provideObserveConversationListDetails(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationListDetails

    @ViewModelScoped
    @Provides
    fun provideObserveConversationUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.getOneToOneConversation

    @ViewModelScoped
    @Provides
    fun provideGetServerConfigUserCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().fetchServerConfigFromDeepLink

    @ViewModelScoped
    @Provides
    fun provideFetchApiVersionUserCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().fetchApiVersion

    @ViewModelScoped
    @Provides
    fun provideObserveServerConfigUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observeServerConfig

    @ViewModelScoped
    @Provides
    fun provideUpdateApiVersionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().updateApiVersions

    @ViewModelScoped
    @Provides
    fun provideCurrentSessionFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSessionFlow

    @ViewModelScoped
    @Provides
    fun provideSelfClientsUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.selfClients

    @ViewModelScoped
    @Provides
    fun provideMlsKeyPackageCountUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.mlsKeyPackageCountUseCase

    @ViewModelScoped
    @Provides
    fun provideRestartSlowSyncProcessForRecoveryUseCase(
        @CurrentAccount currentAccount: UserId,
        clientScopeProviderFactory: ClientScopeProvider.Factory
    ) =
        clientScopeProviderFactory.create(currentAccount).clientScope.restartSlowSyncProcessForRecoveryUseCase

    @ViewModelScoped
    @Provides
    fun provideGetAvatarAssetUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetAvatarAssetUseCase =
        coreLogic.getSessionScope(currentAccount).users.getPublicAsset

    @ViewModelScoped
    @Provides
    fun provideDeleteAssetUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): DeleteAssetUseCase =
        coreLogic.getSessionScope(currentAccount).users.deleteAsset

    @ViewModelScoped
    @Provides
    fun provideUploadUserAvatarUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.uploadUserAvatar

    @ViewModelScoped
    @Provides
    fun provideObserveConversationDetailsUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationDetails

    @ViewModelScoped
    @Provides
    fun provideDeleteTeamConversationUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.deleteTeamConversation

    @ViewModelScoped
    @Provides
    fun provideObserveIsSelfConversationMemberUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeIsSelfUserMemberUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveConversationInteractionAvailability(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationInteractionAvailabilityUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveConversationMembersUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationMembers

    @ViewModelScoped
    @Provides
    fun provideMembersToMentionUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.getMembersToMention

    @ViewModelScoped
    @Provides
    fun provideObserveUserListByIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveUserListByIdUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.observeUserListById

    @ViewModelScoped
    @Provides
    fun provideGetPaginatedMessagesUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.getPaginatedFlowOfMessagesByConversation

    @ViewModelScoped
    @Provides
    fun provideDeleteClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.deleteClient

    @ViewModelScoped
    @Provides
    fun provideGetOrRegisterClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.getOrRegister

    @ViewModelScoped
    @Provides
    fun providePersistOtherUsersClients(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.persistOtherUserClients

    @ViewModelScoped
    @Provides
    fun provideGetOtherUsersClients(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.getOtherUserClients

    @ViewModelScoped
    @Provides
    fun provideNeedsToRegisterClientUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).client.needsToRegisterClient

    @ViewModelScoped
    @Provides
    fun provideGetIncomingCallsUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.getIncomingCalls

    @ViewModelScoped
    @Provides
    fun provideRequestVideoStreamsUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.requestVideoStreams

    @ViewModelScoped
    @Provides
    fun provideIsLastCallClosedUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.isLastCallClosed

    @ViewModelScoped
    @Provides
    fun provideObserveOngoingCallsUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.observeOngoingCalls

    @ViewModelScoped
    @Provides
    fun provideRejectCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.rejectCall

    @ViewModelScoped
    @Provides
    fun provideAcceptCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.answerCall

    @ViewModelScoped
    @Provides
    fun provideGetSelfUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetSelfUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.getSelfUser

    @ViewModelScoped
    @Provides
    fun provideGetSelfTeamUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetSelfTeamUseCase =
        coreLogic.getSessionScope(currentAccount).team.getSelfTeamUseCase

    @ViewModelScoped
    @Provides
    fun provideSendTextMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendTextMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendTextMessage

    @ViewModelScoped
    @Provides
    fun provideSendEditTextMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendEditTextMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendEditTextMessage

    @ViewModelScoped
    @Provides
    fun provideRetryFailedMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): RetryFailedMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.retryFailedMessage

    @ViewModelScoped
    @Provides
    fun provideSendKnockUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendKnockUseCase = coreLogic.getSessionScope(currentAccount).messages.sendKnock

    @ViewModelScoped
    @Provides
    fun provideToggleReactionUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ToggleReactionUseCase = coreLogic.getSessionScope(currentAccount).messages.toggleReaction

    @ViewModelScoped
    @Provides
    fun provideObserveMessageReactionsUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveMessageReactionsUseCase = coreLogic.getSessionScope(currentAccount).messages.observeMessageReactions

    @ViewModelScoped
    @Provides
    fun provideObserveMessageReceiptsUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveMessageReceiptsUseCase = coreLogic.getSessionScope(currentAccount).messages.observeMessageReceipts

    @ViewModelScoped
    @Provides
    fun providesSendAssetMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ScheduleNewAssetMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendAssetMessage

    @ViewModelScoped
    @Provides
    fun provideGetPrivateAssetUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetMessageAssetUseCase = coreLogic.getSessionScope(currentAccount).messages.getAssetMessage

    @ViewModelScoped
    @Provides
    fun provideGetNotificationsUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetNotificationsUseCase = coreLogic.getSessionScope(currentAccount).messages.getNotifications

    @ViewModelScoped
    @Provides
    fun provideGetMessageByIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetMessageByIdUseCase = coreLogic.getSessionScope(currentAccount).messages.getMessageById

    @ViewModelScoped
    @Provides
    fun provideSearchUsersUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchPublicUsersUseCase =
        coreLogic.getSessionScope(currentAccount).users.searchUsers

    @ViewModelScoped
    @Provides
    fun provideSearchKnownUsersUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchKnownUsersUseCase =
        coreLogic.getSessionScope(currentAccount).users.searchKnownUsers

    @ViewModelScoped
    @Provides
    fun provideAddAuthenticatedUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic): AddAuthenticatedUserUseCase =
        coreLogic.getGlobalScope().addAuthenticatedAccount

    @ViewModelScoped
    @Provides
    fun provideGetAllContactsUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetAllContactsUseCase =
        coreLogic.getSessionScope(currentAccount).users.getAllKnownUsers

    @ViewModelScoped
    @Provides
    fun provideGetKnownUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetKnownUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.getKnownUser

    @ViewModelScoped
    @Provides
    fun provideDeleteMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): DeleteMessageUseCase =
        coreLogic.getSessionScope(currentAccount).messages.deleteMessage

    @ViewModelScoped
    @Provides
    fun provideGetOrCreateOneToOneConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetOrCreateOneToOneConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.getOrCreateOneToOneConversationUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveCallByConversationIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetAllCallsWithSortedParticipantsUseCase = coreLogic.getSessionScope(currentAccount).calls.allCallsWithSortedParticipants

    @ViewModelScoped
    @Provides
    fun provideOnGoingCallUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveEstablishedCallsUseCase =
        coreLogic.getSessionScope(currentAccount).calls.establishedCall

    @ViewModelScoped
    @Provides
    fun provideStartCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): StartCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.startCall

    @ViewModelScoped
    @Provides
    fun provideEndCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): EndCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.endCall

    @ViewModelScoped
    @Provides
    fun provideEndCallOnConversationChangeUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): EndCallOnConversationChangeUseCase =
        coreLogic.getSessionScope(currentAccount).calls.endCallOnConversationChange

    @ViewModelScoped
    @Provides
    fun provideMuteCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): MuteCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.muteCall

    @ViewModelScoped
    @Provides
    fun provideUnMuteCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): UnMuteCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.unMuteCall

    @ViewModelScoped
    @Provides
    fun provideSetVideoPreviewUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SetVideoPreviewUseCase = coreLogic.getSessionScope(currentAccount).calls.setVideoPreview

    @ViewModelScoped
    @Provides
    fun provideFlipToBackCameraUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FlipToBackCameraUseCase = coreLogic.getSessionScope(currentAccount).calls.flipToBackCamera

    @ViewModelScoped
    @Provides
    fun provideFlipToFrontCameraUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FlipToFrontCameraUseCase = coreLogic.getSessionScope(currentAccount).calls.flipToFrontCamera

    @ViewModelScoped
    @Provides
    fun turnLoudSpeakerOffUseCaseProvider(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): TurnLoudSpeakerOffUseCase = coreLogic.getSessionScope(currentAccount).calls.turnLoudSpeakerOff

    @ViewModelScoped
    @Provides
    fun provideTurnLoudSpeakerOnUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): TurnLoudSpeakerOnUseCase = coreLogic.getSessionScope(currentAccount).calls.turnLoudSpeakerOn

    @ViewModelScoped
    @Provides
    fun provideObserveSpeakerUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSpeakerUseCase = coreLogic.getSessionScope(currentAccount).calls.observeSpeaker

    @ViewModelScoped
    @Provides
    fun provideUpdateVideoStateUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateVideoStateUseCase =
        coreLogic.getSessionScope(currentAccount).calls.updateVideoState

    @ViewModelScoped
    @Provides
    fun provideCreateGroupConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): CreateGroupConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.createGroupConversation

    @ViewModelScoped
    @Provides
    fun provideAddMemberToConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): AddMemberToConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.addMemberToConversationUseCase

    @ViewModelScoped
    @Provides
    fun provideAddServiceToConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): AddServiceToConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.addServiceToConversationUseCase

    @ViewModelScoped
    @Provides
    fun provideRemoveMemberFromConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): RemoveMemberFromConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.removeMemberFromConversation

    @ViewModelScoped
    @Provides
    fun provideObserveAllServicesUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveAllServicesUseCase =
        coreLogic.getSessionScope(currentAccount).service.observeAllServices

    @ViewModelScoped
    @Provides
    fun provideSearchServicesByNameUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchServicesByNameUseCase =
        coreLogic.getSessionScope(currentAccount).service.searchServicesByName

    @ViewModelScoped
    @Provides
    fun provideLeaveConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): LeaveConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.leaveConversation

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationMutedStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationMutedStatusUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.updateConversationMutedStatus

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationReceiptModeUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationReceiptModeUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.updateConversationReceiptMode

    @ViewModelScoped
    @Provides
    fun provideMarkMessagesAsNotifiedUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.markMessagesAsNotified

    @ViewModelScoped
    @Provides
    fun provideUpdateAssetMessageDownloadStatusUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.updateAssetMessageDownloadStatus

    @ViewModelScoped
    @Provides
    fun provideObservePersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic
    ) = coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus

    @ViewModelScoped
    @Provides
    fun providePersistPersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ) = coreLogic.getSessionScope(currentAccount).persistPersistentWebSocketConnectionStatus

    @ViewModelScoped
    @Provides
    fun provideGetPersistentWebSocketStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ) = coreLogic.getSessionScope(currentAccount).getPersistentWebSocketStatus

    @ViewModelScoped
    @Provides
    fun provideGetUserInfoUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetUserInfoUseCase =
        coreLogic.getSessionScope(currentAccount).users.getUserInfo

    @ViewModelScoped
    @Provides
    fun provideUpdateSelfAvailabilityStatusUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.updateSelfAvailabilityStatus

    @ViewModelScoped
    @Provides
    fun provideIsMLSEnabledUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).isMLSEnabled

    @ViewModelScoped
    @Provides
    fun provideIsFileSharingEnabledUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).isFileSharingEnabled

    @ViewModelScoped
    @Provides
    fun provideFileSharingStatusFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeFileSharingStatus

    @ViewModelScoped
    @Provides
    fun fileSystemProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): KaliumFileSystem =
        coreLogic.getSessionScope(currentAccount).kaliumFileSystem

    @ViewModelScoped
    @Provides
    fun provideGetAllContactsNotInTheConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetAllContactsNotInConversationUseCase =
        coreLogic.getSessionScope(currentAccount).users.getAllContactsNotInConversation

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationAccessUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationAccessRoleUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.updateConversationAccess

    @ViewModelScoped
    @Provides
    fun provideFederatedIdMapper(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FederatedIdMapper =
        coreLogic.getSessionScope(currentAccount).federatedIdMapper

    @ViewModelScoped
    @Provides
    fun provideQualifiedIdMapper(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): QualifiedIdMapper =
        coreLogic.getSessionScope(currentAccount).qualifiedIdMapper

    @ViewModelScoped
    @Provides
    fun provideIsPasswordRequiredUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): IsPasswordRequiredUseCase = coreLogic.getSessionScope(currentAccount).users.isPasswordRequired

    @ViewModelScoped
    @Provides
    fun provideIsReadOnlyAccountUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): IsReadOnlyAccountUseCase = coreLogic.getSessionScope(currentAccount).users.isReadOnlyAccount

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationMemberRoleUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationMemberRoleUseCase = coreLogic.getSessionScope(currentAccount).conversations.updateConversationMemberRole

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationReadDateUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationReadDateUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.updateConversationReadDateUseCase

    @ViewModelScoped
    @Provides
    fun provideBlockUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): BlockUserUseCase = coreLogic.getSessionScope(currentAccount).connection.blockUser

    @ViewModelScoped
    @Provides
    fun provideObserveUserInfoUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveUserInfoUseCase = coreLogic.getSessionScope(currentAccount).users.observeUserInfo

    @ViewModelScoped
    @Provides
    fun provideUnblockUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UnblockUserUseCase = coreLogic.getSessionScope(currentAccount).connection.unblockUser

    @ViewModelScoped
    @Provides
    fun provideSelfServerConfig(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SelfServerConfigUseCase = coreLogic.getSessionScope(currentAccount).users.serverLinks

    @ViewModelScoped
    @Provides
    fun provideObserveValidAccountsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveValidAccountsUseCase =
        coreLogic.getGlobalScope().observeValidAccounts

    @ViewModelScoped
    @Provides
    fun provideObserveCurrentClientUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveCurrentClientIdUseCase =
        coreLogic.getSessionScope(currentAccount).client.observeCurrentClientId

    @ViewModelScoped
    @Provides
    fun observeSecurityClassificationLabelUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSecurityClassificationLabelUseCase =
        coreLogic.getSessionScope(currentAccount).observeSecurityClassificationLabel

    @ViewModelScoped
    @Provides
    fun provideIsCallRunningUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.isCallRunning

    @ViewModelScoped
    @Provides
    fun provideCreateBackupUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).createBackup

    @ViewModelScoped
    @Provides
    fun provideVerifyBackupUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).verifyBackupUseCase

    @ViewModelScoped
    @Provides
    fun provideRestoreBackupUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).restoreBackup

    @ViewModelScoped
    @Provides
    fun provideIsEligibleToStartCall(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.isEligibleToStartCall

    @ViewModelScoped
    @Provides
    fun provideClearConversationContentUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ClearConversationContentUseCase = coreLogic.getSessionScope(currentAccount).conversations.clearConversationContent

    @ViewModelScoped
    @Provides
    fun provideRenameConversation(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.renameConversation

    @ViewModelScoped
    @Provides
    fun provideUpdateApiVersionsScheduler(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.updateApiVersionsScheduler

    @ViewModelScoped
    @Provides
    fun provideObserveReadReceiptsEnabled(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.observeReadReceiptsEnabled

    @ViewModelScoped
    @Provides
    fun providePersistReadReceiptsStatusConfig(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.persistReadReceiptsStatusConfig

    @ViewModelScoped
    @Provides
    fun provideObserveIfAppFreshEnoughUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observeIfAppUpdateRequired

    @ViewModelScoped
    @Provides
    fun provideIsSelfATeamMemberUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ) = coreLogic.getSessionScope(currentAccount).team.isSelfATeamMember

    @ViewModelScoped
    @Provides
    fun provideResetSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): ResetSessionUseCase =
        coreLogic.getSessionScope(currentAccount).messages.resetSession

    @ViewModelScoped
    @Provides
    fun provideUpdateDisplayNameUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateDisplayNameUseCase =
        coreLogic.getSessionScope(currentAccount).users.updateDisplayName

    @ViewModelScoped
    @Provides
    fun provideGetAssetSizeLimitUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetAssetSizeLimitUseCase =
        coreLogic.getSessionScope(currentAccount).users.getAssetSizeLimit

    @ViewModelScoped
    @Provides
    fun provideMarkFileSharingStatusAsNotified(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkFileSharingChangeAsNotifiedUseCase = coreLogic.getSessionScope(currentAccount).markFileSharingStatusAsNotified

    @ViewModelScoped
    @Provides
    fun provideMarkSelfDeletingMessagesAsNotified(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkSelfDeletionStatusAsNotifiedUseCase = coreLogic.getSessionScope(currentAccount).markSelfDeletingMessagesAsNotified

    @ViewModelScoped
    @Provides
    fun provideObserveTeamSettingsSelfDeletionStatusFlagUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveTeamSettingsSelfDeletingStatusUseCase = coreLogic.getSessionScope(currentAccount).observeTeamSettingsSelfDeletionStatus

    @ViewModelScoped
    @Provides
    fun provideObserveSelfDeletionTimerSettingsForConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSelfDeletionTimerSettingsForConversationUseCase = coreLogic.getSessionScope(currentAccount).observeSelfDeletingMessages

    @ViewModelScoped
    @Provides
    fun providePersistNewSelfDeletingMessagesUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): PersistNewSelfDeletionTimerUseCase = coreLogic.getSessionScope(currentAccount).persistNewSelfDeletionStatus

    @ViewModelScoped
    @Provides
    fun provideImageUtil(): ImageUtil = ImageUtil

    @ViewModelScoped
    @Provides
    fun provideGetClientDetailsUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveClientDetailsUseCase = coreLogic.getSessionScope(currentAccount).client.observeClientDetailsUseCase

    @ViewModelScoped
    @Provides
    fun provideGenerateGuestRoomLinkUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GenerateGuestRoomLinkUseCase = coreLogic.getSessionScope(currentAccount).conversations.generateGuestRoomLink

    @ViewModelScoped
    @Provides
    fun provideRevokeGuestRoomLinkUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): RevokeGuestRoomLinkUseCase = coreLogic.getSessionScope(currentAccount).conversations.revokeGuestRoomLink

    @ViewModelScoped
    @Provides
    fun provideObserveGuestRoomLinkUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveGuestRoomLinkUseCase = coreLogic.getSessionScope(currentAccount).conversations.observeGuestRoomLink

    @ViewModelScoped
    @Provides
    fun provideObserveGuestRoomLinkFeatureFlagUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeGuestRoomLinkFeatureFlag

    @ViewModelScoped
    @Provides
    fun provideUpdateMessageTimerUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.updateMessageTimer

    @ViewModelScoped
    @Provides
    fun provideMarkGuestLinkFeatureFlagAsNotChangedUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).markGuestLinkFeatureFlagAsNotChanged

    @ViewModelScoped
    @Provides
    fun provideGetOtherUserSecurityClassificationLabelUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveOtherUserSecurityClassificationLabelUseCase =
        coreLogic.getSessionScope(currentAccount).getOtherUserSecurityClassificationLabel

    @ViewModelScoped
    @Provides
    fun provideObserveNewClientsUseCaseUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observeNewClientsUseCase

    @ViewModelScoped
    @Provides
    fun provideClearNewClientsForUser(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().clearNewClientsForUser

    @ViewModelScoped
    @Provides
    fun provideEnqueueMessageSelfDeletionUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): EnqueueMessageSelfDeletionUseCase = coreLogic.getSessionScope(currentAccount).messages.enqueueMessageSelfDeletion

    @ViewModelScoped
    @Provides
    fun provideClientFingerPrintUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ClientFingerprintUseCase =
        coreLogic.getSessionScope(currentAccount).client.remoteClientFingerPrint

    @ViewModelScoped
    @Provides
    fun provideUpdateClientVerificationStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateClientVerificationStatusUseCase =
        coreLogic.getSessionScope(currentAccount).client.updateClientVerificationStatus

    @ViewModelScoped
    @Provides
    fun provideUpdateEmailUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateEmailUseCase =
        coreLogic.getSessionScope(currentAccount).users.updateEmail

    @ViewModelScoped
    @Provides
    fun provideGetConversationUnreadEventsCountUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetConversationUnreadEventsCountUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.getConversationUnreadEventsCountUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveIsServiceMemberUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveIsServiceMemberUseCase =
        coreLogic.getSessionScope(currentAccount).service.observeIsServiceMember

    @ViewModelScoped
    @Provides
    fun provideGetServiceByIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetServiceByIdUseCase =
        coreLogic.getSessionScope(currentAccount).service.getServiceById

    @ViewModelScoped
    @Provides
    fun provideRefreshUsersWithoutMetadataUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): RefreshUsersWithoutMetadataUseCase = coreLogic.getSessionScope(currentAccount).users.refreshUsersWithoutMetadata

    @ViewModelScoped
    @Provides
    fun provideRefreshConversationsWithoutMetadataUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): RefreshConversationsWithoutMetadataUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.refreshConversationsWithoutMetadata

    @ViewModelScoped
    @Provides
    fun provideDeleteAccountUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): DeleteAccountUseCase =
        coreLogic.getSessionScope(currentAccount).users.deleteAccount

    @ViewModelScoped
    @Provides
    fun provideSendButtonActionMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendButtonActionMessageUseCase =
        coreLogic.getSessionScope(currentAccount).messages.sendButtonActionMessage

    @ViewModelScoped
    @Provides
    fun providePersistScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): PersistScreenshotCensoringConfigUseCase = coreLogic.getSessionScope(currentAccount).persistScreenshotCensoringConfig

    @ViewModelScoped
    @Provides
    fun provideObserveScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveScreenshotCensoringConfigUseCase = coreLogic.getSessionScope(currentAccount).observeScreenshotCensoringConfig

    @ViewModelScoped
    @Provides
    fun provideGetConversationVerificationStatusUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).getConversationVerificationStatus

    @ViewModelScoped
    @Provides
    fun providesJoinConversationViaCodeUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.joinConversationViaCode

    @ViewModelScoped
    @Provides
    fun providesCanCreatePasswordProtectedLinksUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.canCreatePasswordProtectedLinks
}
