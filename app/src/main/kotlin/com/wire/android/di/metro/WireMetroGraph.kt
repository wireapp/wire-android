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
package com.wire.android.di.metro

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModelFactory
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModelFactory
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsViewModelFactory
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModelFactory
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentViewModelFactory
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModelFactory
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModelFactory
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModelFactory
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModelFactory
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModelFactory
import com.wire.android.ui.home.conversations.folder.NewFolderViewModelFactory
import com.wire.android.ui.home.settings.about.dependencies.AndroidDependenciesInfoProvider
import com.wire.android.ui.home.settings.about.dependencies.DependenciesInfoProvider
import com.wire.android.ui.home.settings.about.dependencies.DependenciesViewModelFactory
import com.wire.android.ui.home.settings.about.licenses.AndroidLicensesProvider
import com.wire.android.ui.home.settings.about.licenses.LicensesProvider
import com.wire.android.ui.home.settings.about.licenses.LicensesViewModelFactory
import com.wire.android.ui.home.settings.appsettings.networkSettings.AndroidNetworkSettingsDefaultsProvider
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsDefaultsProvider
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsViewModelFactory
import com.wire.android.ui.home.settings.appearance.CustomizationViewModelFactory
import com.wire.android.ui.home.settings.SettingsViewModelFactory
import com.wire.android.ui.home.settings.account.MyAccountViewModelFactory
import com.wire.android.ui.home.settings.account.color.ChangeUserColorViewModelFactory
import com.wire.android.ui.home.settings.account.deleteAccount.DeleteAccountViewModelFactory
import com.wire.android.ui.home.settings.account.displayname.ChangeDisplayNameViewModelFactory
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailViewModelFactory
import com.wire.android.ui.home.settings.account.handle.ChangeHandleViewModelFactory
import com.wire.android.ui.home.whatsnew.AndroidReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.ReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.WhatsNewViewModelFactory
import com.wire.android.ui.initialsync.InitialSyncViewModelFactory
import com.wire.android.ui.settings.about.AboutThisAppInfoProvider
import com.wire.android.ui.settings.about.AboutThisAppViewModelFactory
import com.wire.android.ui.settings.about.AndroidAboutThisAppInfoProvider
import com.wire.android.ui.settings.devices.SelfDevicesViewModelFactory
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModelFactory
import com.wire.android.ui.userprofile.avatarpicker.AndroidAvatarImageGateway
import com.wire.android.ui.userprofile.avatarpicker.AvatarImageGateway
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModelFactory
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollment
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.conversation.ConversationScope
import com.wire.kalium.logic.feature.conversation.folder.CreateConversationFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsUseCase
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserMlsClientIdentitiesUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.team.TeamScope
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.UpdateAccentColorUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import com.wire.kalium.logic.feature.user.UserScope
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.runBlocking

abstract class WireMetroScope private constructor()

@DependencyGraph(WireMetroScope::class)
@Suppress("TooManyFunctions")
interface WireMetroGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides @ApplicationContext context: Context): WireMetroGraph
    }

    val checkAssetRestrictionsViewModelFactory: CheckAssetRestrictionsViewModelFactory
    val aboutThisAppViewModelFactory: AboutThisAppViewModelFactory
    val createAccountSummaryViewModelFactory: CreateAccountSummaryViewModelFactory
    val whatsNewViewModelFactory: WhatsNewViewModelFactory
    val dependenciesViewModelFactory: DependenciesViewModelFactory
    val licensesViewModelFactory: LicensesViewModelFactory
    val conversationCryptoStatsViewModelFactory: ConversationCryptoStatsViewModelFactory
    val debugFeatureFlagsViewModelFactory: DebugFeatureFlagsViewModelFactory
    val customizationViewModelFactory: CustomizationViewModelFactory
    val initialSyncViewModelFactory: InitialSyncViewModelFactory
    val networkSettingsViewModelFactory: NetworkSettingsViewModelFactory
    val e2EIEnrollmentViewModelFactory: E2EIEnrollmentViewModelFactory
    val getE2EICertificateViewModelFactory: GetE2EICertificateViewModelFactory
    val clearSessionViewModelFactory: ClearSessionViewModelFactory
    val settingsViewModelFactory: SettingsViewModelFactory
    val selfDevicesViewModelFactory: SelfDevicesViewModelFactory
    val avatarPickerViewModelFactory: AvatarPickerViewModelFactory
    val changeUserColorViewModelFactory: ChangeUserColorViewModelFactory
    val changeEmailViewModelFactory: ChangeEmailViewModelFactory
    val changeDisplayNameViewModelFactory: ChangeDisplayNameViewModelFactory
    val changeHandleViewModelFactory: ChangeHandleViewModelFactory
    val myAccountViewModelFactory: MyAccountViewModelFactory
    val deleteAccountViewModelFactory: DeleteAccountViewModelFactory
    val appUnlockWithBiometricsViewModelFactory: AppUnlockWithBiometricsViewModelFactory
    val enterLockScreenViewModelFactory: EnterLockScreenViewModelFactory
    val newFolderViewModelFactory: NewFolderViewModelFactory
    val forgotLockScreenViewModelFactory: ForgotLockScreenViewModelFactory
    val setLockScreenViewModelFactory: SetLockScreenViewModelFactory

    val dispatcherProvider: DispatcherProvider

    @get:CurrentAccount
    val currentAccount: UserId

    @Provides
    fun provideWireMetroHiltEntryPoint(
        @ApplicationContext context: Context,
    ): WireMetroHiltEntryPoint =
        EntryPointAccessors.fromApplication(context, WireMetroHiltEntryPoint::class.java)

    @Provides
    fun provideGlobalDataStore(
        @ApplicationContext context: Context,
    ): GlobalDataStore = GlobalDataStore(context)

    @Provides
    @KaliumCoreLogic
    fun provideKaliumCoreLogic(entryPoint: WireMetroHiltEntryPoint): CoreLogic = entryPoint.coreLogic()

    @Provides
    fun provideUserDataStoreProvider(entryPoint: WireMetroHiltEntryPoint): UserDataStoreProvider =
        entryPoint.userDataStoreProvider()

    @Provides
    fun provideCurrentAccountUserDataStore(
        @CurrentAccount currentAccount: UserId,
        userDataStoreProvider: UserDataStoreProvider,
    ): UserDataStore =
        userDataStoreProvider.getOrCreate(currentAccount)

    @Provides
    fun provideDispatchers(entryPoint: WireMetroHiltEntryPoint): DispatcherProvider = entryPoint.dispatcherProvider()

    @Provides
    fun provideAutomatedLoginManager(entryPoint: WireMetroHiltEntryPoint): AutomatedLoginManager =
        entryPoint.automatedLoginManager()

    @Provides
    fun provideManagedConfigurationsManager(entryPoint: WireMetroHiltEntryPoint): ManagedConfigurationsManager =
        entryPoint.managedConfigurationsManager()

    @Provides
    fun provideLockCodeTimeManager(entryPoint: WireMetroHiltEntryPoint): LockCodeTimeManager =
        entryPoint.lockCodeTimeManager()

    @Provides
    fun provideWireNotificationManager(entryPoint: WireMetroHiltEntryPoint): WireNotificationManager =
        entryPoint.wireNotificationManager()

    @Provides
    fun provideAccountSwitchUseCase(entryPoint: WireMetroHiltEntryPoint): AccountSwitchUseCase =
        entryPoint.accountSwitchUseCase()

    @CurrentAccount
    @Provides
    fun provideCurrentSession(@KaliumCoreLogic coreLogic: CoreLogic): UserId =
        runBlocking {
            when (val result = coreLogic.getGlobalScope().session.currentSession()) {
                is CurrentSessionResult.Success -> result.accountInfo.userId
                else -> throw IllegalStateException("no current session was found")
            }
        }

    @Provides
    fun provideObserveSyncStateUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveSyncStateUseCase =
        coreLogic.getSessionScope(currentAccount).observeSyncState

    @Provides
    fun provideUserScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): UserScope =
        coreLogic.getSessionScope(currentAccount).users

    @Provides
    fun provideGetAvatarAssetUseCase(userScope: UserScope): GetAvatarAssetUseCase =
        userScope.getPublicAsset

    @Provides
    fun provideUploadUserAvatarUseCase(userScope: UserScope): UploadUserAvatarUseCase =
        userScope.uploadUserAvatar

    @Provides
    fun provideKaliumFileSystem(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): KaliumFileSystem =
        coreLogic.getSessionScope(currentAccount).kaliumFileSystem

    @Provides
    fun provideQualifiedIdMapper(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): QualifiedIdMapper =
        coreLogic.getSessionScope(currentAccount).qualifiedIdMapper

    @Provides
    fun provideAvatarImageManager(
        @ApplicationContext context: Context,
    ): AvatarImageManager =
        AvatarImageManager(context)

    @Provides
    fun provideAvatarImageGateway(
        avatarImageManager: AvatarImageManager,
        dispatchers: DispatcherProvider,
        @ApplicationContext context: Context,
    ): AvatarImageGateway =
        AndroidAvatarImageGateway(
            avatarImageManager = avatarImageManager,
            dispatchers = dispatchers,
            appContext = context,
        )

    @Provides
    fun provideGetFeatureConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): GetFeatureConfigUseCase =
        coreLogic.getSessionScope(currentAccount).debug.getFeatureConfig

    @Provides
    fun provideGetConversationCryptoStatsUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): GetConversationCryptoStatsUseCase =
        coreLogic.getSessionScope(currentAccount).debug.getConversationCryptoStats

    @Provides
    fun provideObserveIsAppLockEditableUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveIsAppLockEditableUseCase =
        coreLogic.getGlobalScope().observeIsAppLockEditableUseCase

    @Provides
    fun provideObserveSelfUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveSelfUserUseCase =
        coreLogic.getSessionScope(currentAccount).users.observeSelfUser

    @Provides
    fun provideObserveSelfUserWithTeamUseCase(userScope: UserScope): ObserveSelfUserWithTeamUseCase =
        userScope.observeSelfUserWithTeam

    @Provides
    fun provideGetSelfUserUseCase(userScope: UserScope): GetSelfUserUseCase =
        userScope.getSelfUser

    @Provides
    fun provideSelfServerConfigUseCase(userScope: UserScope): SelfServerConfigUseCase =
        userScope.serverLinks

    @Provides
    fun provideIsPasswordRequiredUseCase(userScope: UserScope): IsPasswordRequiredUseCase =
        userScope.isPasswordRequired

    @Provides
    fun provideIsReadOnlyAccountUseCase(userScope: UserScope): IsReadOnlyAccountUseCase =
        userScope.isReadOnlyAccount

    @Provides
    fun provideDeleteAccountUseCase(userScope: UserScope): DeleteAccountUseCase =
        userScope.deleteAccount

    @Provides
    fun provideSetUserHandleUseCase(userScope: UserScope): SetUserHandleUseCase =
        userScope.setUserHandle

    @Provides
    fun provideValidateUserHandleUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ValidateUserHandleUseCase =
        coreLogic.getGlobalScope().validateUserHandleUseCase

    @Provides
    fun provideValidatePasswordUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ValidatePasswordUseCase =
        coreLogic.getGlobalScope().validatePasswordUseCase

    @Provides
    fun provideObserveAppLockConfigUseCase(
        globalDataStore: GlobalDataStore,
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ObserveAppLockConfigUseCase =
        ObserveAppLockConfigUseCase(
            globalDataStore = globalDataStore,
            coreLogic = coreLogic,
        )

    @Provides
    fun provideMarkTeamAppLockStatusAsNotifiedUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): MarkTeamAppLockStatusAsNotifiedUseCase =
        coreLogic.getSessionScope(currentAccount).markTeamAppLockStatusAsNotified

    @Provides
    fun provideUpdateEmailUseCase(userScope: UserScope): UpdateEmailUseCase =
        userScope.updateEmail

    @Provides
    fun provideUpdateAccentColorUseCase(userScope: UserScope): UpdateAccentColorUseCase =
        userScope.updateAccentColor

    @Provides
    fun provideUpdateDisplayNameUseCase(userScope: UserScope): UpdateDisplayNameUseCase =
        userScope.updateDisplayName

    @Provides
    fun provideFinalizeMLSClientAfterE2EIEnrollmentUseCase(userScope: UserScope): FinalizeMLSClientAfterE2EIEnrollment =
        userScope.finalizeMLSClientAfterE2EIEnrollment

    @Provides
    fun provideDeleteSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DeleteSessionUseCase =
        coreLogic.getGlobalScope().deleteSession

    @Provides
    fun provideLogoutUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): LogoutUseCase =
        coreLogic.getSessionScope(currentAccount).logout

    @Provides
    fun provideFetchSelfClientsFromRemoteUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): FetchSelfClientsFromRemoteUseCase =
        coreLogic.getSessionScope(currentAccount).client.fetchSelfClients

    @Provides
    fun provideObserveClientsByUserIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveClientsByUserIdUseCase =
        coreLogic.getSessionScope(currentAccount).client.getOtherUserClients

    @Provides
    fun provideObserveCurrentClientIdUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveCurrentClientIdUseCase =
        coreLogic.getSessionScope(currentAccount).client.observeCurrentClientId

    @Provides
    fun provideGetUserMlsClientIdentitiesUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): GetUserMlsClientIdentitiesUseCase =
        coreLogic.getSessionScope(currentAccount).users.getUserMlsClientIdentities

    @Provides
    fun provideIsE2EIEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): IsE2EIEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isE2EIEnabled

    @Provides
    fun provideTeamScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): TeamScope =
        coreLogic.getSessionScope(currentAccount).team

    @Provides
    fun provideIsSelfATeamMemberUseCase(teamScope: TeamScope): IsSelfATeamMemberUseCase =
        teamScope.isSelfATeamMember

    @Provides
    fun provideConversationScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ConversationScope =
        coreLogic.getSessionScope(currentAccount).conversations

    @Provides
    fun provideObserveUserFoldersUseCase(conversationScope: ConversationScope): ObserveUserFoldersUseCase =
        conversationScope.observeUserFolders

    @Provides
    fun provideCreateConversationFolderUseCase(conversationScope: ConversationScope): CreateConversationFolderUseCase =
        conversationScope.createConversationFolder

    @Provides
    fun provideGetSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): GetSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessions

    @Provides
    fun provideCallsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): CallsScope =
        coreLogic.getSessionScope(currentAccount).calls

    @Provides
    fun provideObserveEstablishedCallsUseCase(callsScope: CallsScope): ObserveEstablishedCallsUseCase =
        callsScope.establishedCall

    @Provides
    fun provideEndCallUseCase(callsScope: CallsScope): EndCallUseCase =
        callsScope.endCall

    @Provides
    fun provideNetworkSettingsDefaultsProvider(
        @ApplicationContext context: Context,
    ): NetworkSettingsDefaultsProvider = AndroidNetworkSettingsDefaultsProvider(context)

    @Provides
    fun provideCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): CurrentSessionUseCase =
        coreLogic.getGlobalScope().session.currentSession

    @Provides
    fun provideObservePersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ObservePersistentWebSocketConnectionStatusUseCase =
        coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus

    @Provides
    fun providePersistPersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): PersistPersistentWebSocketConnectionStatusUseCase =
        coreLogic.getSessionScope(currentAccount).persistPersistentWebSocketConnectionStatus

    @Provides
    fun provideAboutThisAppInfoProvider(
        @ApplicationContext context: Context,
    ): AboutThisAppInfoProvider = AndroidAboutThisAppInfoProvider(context)

    @Provides
    fun provideReleaseNotesFeedUrlProvider(
        @ApplicationContext context: Context,
    ): ReleaseNotesFeedUrlProvider = AndroidReleaseNotesFeedUrlProvider(context)

    @Provides
    fun provideDependenciesInfoProvider(
        @ApplicationContext context: Context,
    ): DependenciesInfoProvider = AndroidDependenciesInfoProvider(context)

    @Provides
    fun provideLicensesProvider(
        @ApplicationContext context: Context,
    ): LicensesProvider = AndroidLicensesProvider(context)
}

fun createWireMetroGraph(context: Context): WireMetroGraph =
    createGraphFactory<WireMetroGraph.Factory>().create(context.applicationContext)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WireMetroHiltEntryPoint {
    @KaliumCoreLogic
    fun coreLogic(): CoreLogic

    fun userDataStoreProvider(): UserDataStoreProvider

    fun dispatcherProvider(): DispatcherProvider

    fun automatedLoginManager(): AutomatedLoginManager

    fun managedConfigurationsManager(): ManagedConfigurationsManager

    fun lockCodeTimeManager(): LockCodeTimeManager

    fun wireNotificationManager(): WireNotificationManager

    fun accountSwitchUseCase(): AccountSwitchUseCase
}
