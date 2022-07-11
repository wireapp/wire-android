package com.wire.android.di

import android.content.Context
import com.wire.android.util.DeviceLabel
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
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
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
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
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
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
annotation class CurrentAccount

@Module
@InstallIn(SingletonComponent::class)
class CoreLogicModule {

    @KaliumCoreLogic
    @Singleton
    @Provides
    fun coreLogicProvider(@ApplicationContext context: Context, kaliumConfigs: KaliumConfigs): CoreLogic {
        val rootPath = context.getDir("accounts", Context.MODE_PRIVATE).path
        val deviceLabel = DeviceLabel.label

        return CoreLogic(
            appContext = context,
            rootPath = rootPath,
            clientLabel = deviceLabel,
            kaliumConfigs = kaliumConfigs
        )
    }
}

@Module
@InstallIn(ViewModelComponent::class)
class SessionModule {
    @CurrentAccount
    @ViewModelScoped
    @Provides
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    fun currentSessionProvider(@KaliumCoreLogic coreLogic: CoreLogic): UserId {
        return runBlocking {
            return@runBlocking when (val result = coreLogic.getGlobalScope().session.currentSession.invoke()) {
                is CurrentSessionResult.Success -> result.authSession.tokens.userId
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
    fun sendConnectionRequestUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.sendConnectionRequest

    @ViewModelScoped
    @Provides
    fun cancelConnectionRequestUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.cancelConnectionRequest

    @ViewModelScoped
    @Provides
    fun ignoreConnectionRequestUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.ignoreConnectionRequest

    @ViewModelScoped
    @Provides
    fun acceptConnectionRequestUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).connection.acceptConnectionRequest
}

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions")
class UseCaseModule {

    @ViewModelScoped
    @Provides
    fun loginUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).login

    @ViewModelScoped
    @Provides
    fun ssoInitiateLoginUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).ssoLoginScope.initiate

    @ViewModelScoped
    @Provides
    fun getLoginSessionUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).ssoLoginScope.getLoginSessionGet

    @ViewModelScoped
    @Provides
    fun observeSyncStateProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): ObserveSyncStateUseCase =
        coreLogic.getSessionScope(currentAccount).observeSyncState

    @ViewModelScoped
    @Provides
    fun logoutUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): LogoutUseCase =
        coreLogic.getSessionScope(currentAccount).logout

    @ViewModelScoped
    @Provides
    fun validateEmailUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).validateEmailUseCase

    @ViewModelScoped
    @Provides
    fun validatePasswordUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).validatePasswordUseCase

    @ViewModelScoped
    @Provides
    fun validateUserHandleUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).validateUserHandleUseCase

    @ViewModelScoped
    @Provides
    fun registerAccountUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).register.register

    @ViewModelScoped
    @Provides
    fun requestCodeUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).register.requestActivationCode

    @ViewModelScoped
    @Provides
    fun verifyCodeUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, authServerConfigProvider: AuthServerConfigProvider) =
        coreLogic.getAuthenticationScope(authServerConfigProvider.authServer.value).register.activate

    @ViewModelScoped
    @Provides
    fun setUserHandleUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
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
    fun getServerConfigUserCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().fetchServerConfigFromDeepLink

    @ViewModelScoped
    @Provides
    fun observeServerConfigUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observeServerConfig

    @ViewModelScoped
    @Provides
    fun updateApiVersionsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().updateApiVersions

    @ViewModelScoped
    @Provides
    // TODO: kind of redundant to CurrentSession - need to rename CurrentSession
    fun currentSessionUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSession

    @ViewModelScoped
    @Provides
    fun currentSessionFlowUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSessionFlow

    @ViewModelScoped
    @Provides
    fun selfClientsUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.selfClients

    @ViewModelScoped
    @Provides
    fun mlsKeyPackageCountUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.mlsKeyPackageCountUseCase

    @ViewModelScoped
    @Provides
    fun getAvatarAsset(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetAvatarAssetUseCase =
        coreLogic.getSessionScope(currentAccount).users.getPublicAsset

    @ViewModelScoped
    @Provides
    fun uploadUserAvatar(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): UploadUserAvatarUseCase =
        coreLogic.getSessionScope(currentAccount).users.uploadUserAvatar

    @ViewModelScoped
    @Provides
    fun observeConversationDetailsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationDetails

    @ViewModelScoped
    @Provides
    fun observeConversationMembersUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeConversationMembers

    @ViewModelScoped
    @Provides
    fun observeMemberDetailsByIdsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).conversations.observeMemberDetailsByIds

    @ViewModelScoped
    @Provides
    fun getMessagesUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.getRecentMessages

    @ViewModelScoped
    @Provides
    fun deleteClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.deleteClient

    @ViewModelScoped
    @Provides
    fun registerClientUseCase(@CurrentAccount currentAccount: UserId, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.register

    @ViewModelScoped
    @Provides
    fun needsToRegisterClientUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).client.needsToRegisterClient

    @ViewModelScoped
    @Provides
    fun getIncomingCallsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.getIncomingCalls

    @ViewModelScoped
    @Provides
    fun observeOngoingCallsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.observeOngoingCalls

    @ViewModelScoped
    @Provides
    fun rejectCallUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.rejectCall

    @ViewModelScoped
    @Provides
    fun acceptCallUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).calls.answerCall

    @ViewModelScoped
    @Provides
    fun providesGetSelfUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetSelfUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.getSelfUser

    @ViewModelScoped
    @Provides
    fun providesGetSelfTeamUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetSelfTeamUseCase =
        coreLogic.getSessionScope(currentAccount).team.getSelfTeamUseCase

    @ViewModelScoped
    @Provides
    fun providesSendTextMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendTextMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendTextMessage

    @ViewModelScoped
    @Provides
    fun providesSendImageMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendImageMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendImageMessage

    @ViewModelScoped
    @Provides
    fun providesSendAssetMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SendAssetMessageUseCase = coreLogic.getSessionScope(currentAccount).messages.sendAssetMessage

    @ViewModelScoped
    @Provides
    fun providesGetPrivateAssetUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetMessageAssetUseCase = coreLogic.getSessionScope(currentAccount).messages.getAssetMessage

    @ViewModelScoped
    @Provides
    fun providesSearchUsersUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchUsersUseCase =
        coreLogic.getSessionScope(currentAccount).users.searchUsers

    @ViewModelScoped
    @Provides
    fun providesSearchKnownUsersUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): SearchKnownUsersUseCase =
        coreLogic.getSessionScope(currentAccount).users.searchKnownUsers

    @ViewModelScoped
    @Provides
    fun providesSearchPublicUserUseCase(
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
    fun providesGetAllContactsUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetAllContactsUseCase =
        coreLogic.getSessionScope(currentAccount).users.getAllKnownUsers

    @ViewModelScoped
    @Provides
    fun providesGetKnownUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetKnownUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.getKnownUser

    @ViewModelScoped
    @Provides
    fun providesDeleteMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): DeleteMessageUseCase =
        coreLogic.getSessionScope(currentAccount).messages.deleteMessage

    @ViewModelScoped
    @Provides
    fun providesGetOrCreateOneToOneConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetOrCreateOneToOneConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.getOrCreateOneToOneConversationUseCase

    @ViewModelScoped
    @Provides
    fun providesObserveCallByConversationIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetAllCallsWithSortedParticipantsUseCase = coreLogic.getSessionScope(currentAccount).calls.allCallsWithSortedParticipants

    @ViewModelScoped
    @Provides
    fun providesOnGoingCallUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveEstablishedCallsUseCase =
        coreLogic.getSessionScope(currentAccount).calls.establishedCall

    @ViewModelScoped
    @Provides
    fun providesStartCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): StartCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.startCall

    @ViewModelScoped
    @Provides
    fun providesEndCallUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): EndCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.endCall

    @ViewModelScoped
    @Provides
    fun muteCallUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): MuteCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.muteCall

    @ViewModelScoped
    @Provides
    fun unMuteCallUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): UnMuteCallUseCase =
        coreLogic.getSessionScope(currentAccount).calls.unMuteCall

    @ViewModelScoped
    @Provides
    fun setVideoPreviewUseCaseProvider(
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
    fun turnLoudSpeakerOnUseCaseProvider(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): TurnLoudSpeakerOnUseCase = coreLogic.getSessionScope(currentAccount).calls.turnLoudSpeakerOn

    @ViewModelScoped
    @Provides
    fun observeSpeakerUseCaseProvider(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSpeakerUseCase = coreLogic.getSessionScope(currentAccount).calls.observeSpeaker

    @ViewModelScoped
    @Provides
    fun updateVideoStateUseCaseProvider(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateVideoStateUseCase =
        coreLogic.getSessionScope(currentAccount).calls.updateVideoState

    @ViewModelScoped
    @Provides
    fun providesCreateGroupConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): CreateGroupConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.createGroupConversation

    @ViewModelScoped
    @Provides
    fun providesAddMemberToConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): AddMemberToConversationUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.addMemberToConversationUseCase

    @ViewModelScoped
    @Provides
    fun providesUpdateConversationMutedStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UpdateConversationMutedStatusUseCase =
        coreLogic.getSessionScope(currentAccount).conversations.updateConversationMutedStatus

    @ViewModelScoped
    @Provides
    fun markMessagesAsNotifiedUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.markMessagesAsNotified

    @ViewModelScoped
    @Provides
    fun updateAssetMessageDownloadStatusUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).messages.updateAssetMessageDownloadStatus

    @ViewModelScoped
    @Provides
    fun enableLoggingUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().enableLogging

    @ViewModelScoped
    @Provides
    fun isLoggingUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().isLoggingEnabled

    @ViewModelScoped
    @Provides
    fun getUserInfoUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.getUserInfo

    @ViewModelScoped
    @Provides
    fun getBuildConfigUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().buildConfigs

    @ViewModelScoped
    @Provides
    fun updateSelfAvailabilityStatusUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).users.updateSelfAvailabilityStatus

    @ViewModelScoped
    @Provides
    fun getFileSharingStatusProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).getRemoteFeatureConfigsStatusAndPersist

    @ViewModelScoped
    @Provides
    fun isFileSharingEnabledUserCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).isFileSharingEnabled

    @ViewModelScoped
    @Provides
    fun registerTokenUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): RegisterTokenUseCase =
        coreLogic.getSessionScope(currentAccount).client.registerPushToken
}
