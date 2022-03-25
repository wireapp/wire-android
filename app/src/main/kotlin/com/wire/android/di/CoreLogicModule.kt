package com.wire.android.di

import android.content.Context
import com.wire.android.util.DeviceLabel
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUserDirectoryUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
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
    fun coreLogicProvider(@ApplicationContext context: Context): CoreLogic {
        val proteusPath = context.getDir("proteus", Context.MODE_PRIVATE).path
        val deviceLabel = DeviceLabel.label

        return CoreLogic(
            appContext = context,
            rootProteusDirectoryPath = proteusPath,
            clientLabel = deviceLabel
        )
    }
}

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions")
class UseCaseModule {

    @CurrentAccount
    @ViewModelScoped
    @Provides
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    fun currentSessionProvider(@KaliumCoreLogic coreLogic: CoreLogic): String {
        return runBlocking {
            return@runBlocking when (val result = coreLogic.getAuthenticationScope().session.currentSession.invoke()) {
                is CurrentSessionResult.Success -> result.authSession.userId
                else -> {
                    throw IllegalStateException("no current session was found")
                }
            }
        }
    }

    @ViewModelScoped
    @Provides
    fun loginUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().login

    @ViewModelScoped
    @Provides
    fun logoutUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String): LogoutUseCase =
        coreLogic.getSessionScope(currentAccount).logout

    @ViewModelScoped
    @Provides
    fun validateEmailUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().validateEmailUseCase

    @ViewModelScoped
    @Provides
    fun validatePasswordUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().validatePasswordUseCase

    @ViewModelScoped
    @Provides
    fun validateUserHandleUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().validateUserHandleUseCase

    @ViewModelScoped
    @Provides
    fun registerAccountUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().register.register

    @ViewModelScoped
    @Provides
    fun requestCodeUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().register.requestActivationCode

    @ViewModelScoped
    @Provides
    fun verifyCodeUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().register.activate

    @ViewModelScoped
    @Provides
    fun setUserHandleUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String) =
        coreLogic.getSessionScope(currentAccount).users.setUserHandle

    @ViewModelScoped
    @Provides
    fun getConversationsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String) =
        coreLogic.getSessionScope(currentAccount).conversations.getConversations

    @ViewModelScoped
    @Provides
    fun getServerConfigUserCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getAuthenticationScope().getServerConfig

    @ViewModelScoped
    @Provides
    // TODO: kind of redundant to CurrentSession - need to rename CurrentSession
    fun currentSessionUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) = coreLogic.getAuthenticationScope().session.currentSession

    @ViewModelScoped
    @Provides
    fun selfClientsUseCase(@CurrentAccount currentAccount: String, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.selfClients

    @ViewModelScoped
    @Provides
    fun getPublicAsset(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String): GetPublicAssetUseCase =
        coreLogic.getSessionScope(currentAccount).users.getPublicAsset

    @ViewModelScoped
    @Provides
    fun uploadUserAvatar(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String): UploadUserAvatarUseCase =
        coreLogic.getSessionScope(currentAccount).users.uploadUserAvatar

    @ViewModelScoped
    @Provides
    fun getConversationDetailsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String) =
        coreLogic.getSessionScope(currentAccount).conversations.getConversationDetails

    @ViewModelScoped
    @Provides
    fun getMessagesUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String) =
        coreLogic.getSessionScope(currentAccount).messages.getRecentMessages

    @ViewModelScoped
    @Provides
    fun deleteClientUseCase(@CurrentAccount currentAccount: String, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.deleteClient

    @ViewModelScoped
    @Provides
    fun registerClientUseCase(@CurrentAccount currentAccount: String, clientScopeProviderFactory: ClientScopeProvider.Factory) =
        clientScopeProviderFactory.create(currentAccount).clientScope.register

    @ViewModelScoped
    @Provides
    fun listenToEventsUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String) =
        coreLogic.getSessionScope(currentAccount).listenToEvents

    @ViewModelScoped
    @Provides
    fun providesGetSelfUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: String): GetSelfUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.getSelfUser

    @ViewModelScoped
    @Provides
    fun providesSendTextMessageUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: String
    ): SendTextMessageUseCase =
        coreLogic.getSessionScope(currentAccount).messages.sendTextMessage

    @ViewModelScoped
    @Provides
    fun providesSearchKnownUsersUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentSession: AuthSession
    ): SearchKnownUsersUseCase =
        coreLogic.getSessionScope(currentSession.userId).users.searchKnownUsers

    @ViewModelScoped
    @Provides
    fun providesSearchPublicUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentSession: AuthSession
    ): SearchUserDirectoryUseCase =
        coreLogic.getSessionScope(currentSession.userId).users.searchUserDirectory

    @ViewModelScoped
    @Provides
    fun provideLogoutUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentSession: AuthSession): LogoutUseCase =
        coreLogic.getSessionScope(currentSession.userId).logout

}
