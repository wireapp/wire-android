package com.wire.android.di

import android.content.Context
import com.wire.android.util.DeviceLabel
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.FederatedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
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
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.GetKnownUserUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUserDirectoryUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.RegisterTokenUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
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
    fun provideCoreLogic(@ApplicationContext context: Context, kaliumConfigs: KaliumConfigs): CoreLogic {
        val rootPath = context.getDir("accounts", Context.MODE_PRIVATE).path
        val deviceLabel = DeviceLabel.label

        return CoreLogic(
            appContext = context,
            rootPath = rootPath,
            clientLabel = deviceLabel,
            kaliumConfigs = kaliumConfigs
        )
    }

    @NoSession
    @Singleton
    @Provides
    fun provideNoSessionQualifiedIdMapper(): QualifiedIdMapper = QualifiedIdMapperImpl(null)

    @NoSession
    @Singleton
    @Provides
    fun provideObservePersistentWebSocketConnectionStatusUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus
}

@Module
@InstallIn(ViewModelComponent::class)
class SessionModule {
    @CurrentAccount
    @ViewModelScoped
    @Provides
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    fun provideCurrentSession(@KaliumCoreLogic coreLogic: CoreLogic): UserId {
        return runBlocking {
            return@runBlocking when (val result = coreLogic.getGlobalScope().session.currentSession.invoke()) {
                is CurrentSessionResult.Success -> result.authSession.session.userId
                else -> {
                    throw IllegalStateException("no current session was found")
                }
            }
        }
    }
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
@Suppress("TooManyFunctions")
class UseCaseModule {

    @ViewModelScoped
    @Provides
    fun provideLoginUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).login

    @ViewModelScoped
    @Provides
    fun provideSsoInitiateLoginUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).ssoLoginScope.initiate

    @ViewModelScoped
    @Provides
    fun provideGetLoginSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).ssoLoginScope.getLoginSessionGet

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
    fun provideValidateEmailUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).validateEmailUseCase

    @ViewModelScoped
    @Provides
    fun provideValidatePasswordUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).validatePasswordUseCase

    @ViewModelScoped
    @Provides
    fun provideValidateUserHandleUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).validateUserHandleUseCase

    @ViewModelScoped
    @Provides
    fun provideRegisterAccountUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).register.register

    @ViewModelScoped
    @Provides
    fun provideRequestCodeUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).register.requestActivationCode

    @ViewModelScoped
    @Provides
    fun provideVerifyCodeUseCase(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).register.activate

    @ViewModelScoped
    @Provides
    fun provideSetUserHandleUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.setUserHandle

    @ViewModelScoped
    @Provides
    fun provideObserveConversationListDetailsUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationListDetails

    @ViewModelScoped
    @Provides
    fun provideObserveConnectionListUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConnectionList

    @ViewModelScoped
    @Provides
    fun provideObserveConversationsAndConnectionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationsAndConnectionListUseCase

    @ViewModelScoped
    @Provides
    fun provideGetServerConfigUserCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().fetchServerConfigFromDeepLink

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
    // TODO: kind of redundant to CurrentSession - need to rename CurrentSession
    fun provideCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSession

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
    fun provideGetAvatarAssetUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetAvatarAssetUseCase =
        coreLogic.getSessionScope(currentAccount).users.getPublicAsset

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
    fun provideObserveConversationMembersUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationMembers

    @ViewModelScoped
    @Provides
    fun provideObserveUserListByIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveUserListByIdUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.observeUserListById

    @ViewModelScoped
    @Provides
    fun provideGetMessagesUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.getRecentMessages

    @ViewModelScoped
    @Provides
    fun provideDeleteClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.deleteClient

    @ViewModelScoped
    @Provides
    fun provideRegisterClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.register

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
    fun providesSendAssetMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendAssetMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendAssetMessage

    @ViewModelScoped
    @Provides
    fun provideGetPrivateAssetUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetMessageAssetUseCase = coreLogic.getSessionScope(currentAccount).messages.getAssetMessage

    @ViewModelScoped
    @Provides
    fun provideSearchUsersUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchUsersUseCase =
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
    fun provideSearchPublicUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchUserDirectoryUseCase =
        coreLogic.getSessionScope(currentAccount).users.searchUserDirectory

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
    fun provideRemoveMemberFromConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): RemoveMemberFromConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.removeMemberFromConversation

    @ViewModelScoped
    @Provides
    fun provideUpdateConversationMutedStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationMutedStatusUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.updateConversationMutedStatus

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
    fun provideEnableLoggingUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().enableLogging

    @ViewModelScoped
    @Provides
    fun provideLoggingUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().isLoggingEnabled

    @ViewModelScoped
    @Provides
    fun provideObservePersistentWebSocketConnectionStatusUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus

    @ViewModelScoped
    @Provides
    fun providePersistPersistentWebSocketConnectionStatusUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().persistPersistentWebSocketConnectionStatus

    @ViewModelScoped
    @Provides
    fun provideGetUserInfoUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetUserInfoUseCase =
        coreLogic.getSessionScope(currentAccount).users.getUserInfo

    @ViewModelScoped
    @Provides
    fun provideGetBuildConfigUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().buildConfigs

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
    fun provideRegisterTokenUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): RegisterTokenUseCase =
        coreLogic.getSessionScope(currentAccount).client.registerPushToken

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
    fun provideUpdateConversationMemberRoleUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationMemberRoleUseCase = coreLogic.getSessionScope(currentAccount).conversations.updateConversationMemberRole

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
}
