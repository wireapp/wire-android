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

package com.wire.android.di

import android.content.Context
import androidx.work.WorkManager
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.UserAgentProvider
import com.wire.android.util.isWebsocketEnabledByDefault
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.FederatedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase
import com.wire.kalium.logic.feature.backup.CreateMPBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreMPBackupUseCase
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ObserveOtherUserSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.FetchConversationMLSVerificationStatusUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldRequestUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForUserUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveTeamSettingsSelfDeletingStatusUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.server.ServerConfigForAccountUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.feature.user.MarkFileSharingChangeAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.MarkSelfDeletionStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.ObserveFileSharingStatusUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.MarkGuestLinkFeatureFlagAsNotChangedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.GetPersistentWebSocketStatus
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.util.RandomPassword
import com.wire.kalium.network.NetworkStateObserver
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.runBlocking
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentAccount

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoSession

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultWebSocketEnabledByDefault

@BindingContainer
class CoreLogicModule {

    @KaliumCoreLogic
    @SingleIn(AppScope::class)
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

    @SingleIn(AppScope::class)
    @Provides
    fun provideNetworkStateObserver(@KaliumCoreLogic coreLogic: CoreLogic): NetworkStateObserver =
        coreLogic.networkStateObserver

    @Provides
    fun provideCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): CurrentSessionUseCase =
        coreLogic.getGlobalScope().session.currentSession

    @Provides
    fun deleteSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DeleteSessionUseCase =
        coreLogic.getGlobalScope().deleteSession

    @Provides
    fun provideUpdateCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): UpdateCurrentSessionUseCase =
        coreLogic.getGlobalScope().session.updateCurrentSession

    @Provides
    fun provideGetAllSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): GetSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessions

    @Provides
    fun provideObserveAllSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessionsFlow

    @Provides
    fun provideServerConfigForAccountUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ServerConfigForAccountUseCase =
        coreLogic.getGlobalScope().serverConfigForAccounts

    @NoSession
    @SingleIn(AppScope::class)
    @Provides
    fun provideNoSessionQualifiedIdMapper(): QualifiedIdMapper = QualifiedIdMapper(null)

    @SingleIn(AppScope::class)
    @Provides
    fun provideWorkManager(@ApplicationContext applicationContext: Context): WorkManager = WorkManager.getInstance(applicationContext)

    @Provides
    fun provideAudioNormalizedLoudnessBuilder(@KaliumCoreLogic coreLogic: CoreLogic): AudioNormalizedLoudnessBuilder =
        coreLogic.audioNormalizedLoudnessBuilder

    @DefaultWebSocketEnabledByDefault
    @Provides
    fun provideDefaultWebSocketEnabledByDefault(
        @ApplicationContext context: Context,
        managedConfigurationsManager: ManagedConfigurationsManager
    ): Boolean = isWebsocketEnabledByDefault(
        context,
        managedConfigurationsManager.persistentWebSocketEnforcedByMDM.value
    )
}

@BindingContainer
class SessionModule {
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped

    @CurrentAccount
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

    @Provides
    fun provideCurrentAccountUserDataStore(
        @CurrentAccount currentAccount: UserId,
        userDataStoreProvider: UserDataStoreProvider
    ): UserDataStore =
        userDataStoreProvider.getOrCreate(currentAccount)
}

@BindingContainer
@Suppress("TooManyFunctions", "LargeClass")
class UseCaseModule {

    @Provides
    fun provideObserveSyncStateUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSyncStateUseCase =
        coreLogic.getSessionScope(currentAccount).observeSyncState

    @Provides
    fun provideLogoutUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): LogoutUseCase =
        coreLogic.getSessionScope(currentAccount).logout

    @Provides
    fun provideValidateEmailUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidateEmailUseCase =
        coreLogic.getGlobalScope().validateEmailUseCase

    @Provides
    fun provideValidateSSOCodeUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidateSSOCodeUseCase =
        coreLogic.getGlobalScope().validateSSOCodeUseCase

    @Provides
    fun provideValidatePasswordUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidatePasswordUseCase =
        coreLogic.getGlobalScope().validatePasswordUseCase

    @Provides
    fun provideValidateUserHandleUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ValidateUserHandleUseCase =
        coreLogic.getGlobalScope().validateUserHandleUseCase

    @Provides
    fun provideGetServerConfigUserCase(@KaliumCoreLogic coreLogic: CoreLogic): GetServerConfigUseCase =
        coreLogic.getGlobalScope().fetchServerConfigFromDeepLink

    @Provides
    fun provideCurrentSessionFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic): CurrentSessionFlowUseCase =
        coreLogic.getGlobalScope().session.currentSessionFlow

    @Provides
    fun provideAddAuthenticatedUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic): AddAuthenticatedUserUseCase =
        coreLogic.getGlobalScope().addAuthenticatedAccount

    @Provides
    fun provideObservePersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic
    ): ObservePersistentWebSocketConnectionStatusUseCase = coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus

    @Provides
    fun providePersistPersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): PersistPersistentWebSocketConnectionStatusUseCase =
        coreLogic.getSessionScope(currentAccount).persistPersistentWebSocketConnectionStatus

    @Provides
    fun provideGetPersistentWebSocketStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetPersistentWebSocketStatus = coreLogic.getSessionScope(currentAccount).getPersistentWebSocketStatus

    @Provides
    fun provideCheckCrlRevocationListUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): CheckCrlRevocationListUseCase =
        coreLogic.getSessionScope(currentAccount).checkCrlRevocationList

    @Provides
    fun provideIsMLSEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): IsMLSEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isMLSEnabled

    @Provides
    fun provideGetDefaultProtocol(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetDefaultProtocolUseCase =
        coreLogic.getSessionScope(currentAccount).getDefaultProtocol

    @Provides
    fun provideIsE2EIEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): IsE2EIEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isE2EIEnabled

    @Provides
    fun provideIsFileSharingEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): IsFileSharingEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isFileSharingEnabled

    @Provides
    fun provideFileSharingStatusFlowUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveFileSharingStatusUseCase =
        coreLogic.getSessionScope(currentAccount).observeFileSharingStatus

    @Provides
    fun fileSystemProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): KaliumFileSystem =
        coreLogic.getSessionScope(currentAccount).kaliumFileSystem

    @Provides
    fun provideFederatedIdMapper(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FederatedIdMapper =
        coreLogic.getSessionScope(currentAccount).federatedIdMapper

    @Provides
    fun provideQualifiedIdMapper(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): QualifiedIdMapper =
        coreLogic.getSessionScope(currentAccount).qualifiedIdMapper

    @Provides
    fun provideBlockUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): BlockUserUseCase = coreLogic.getSessionScope(currentAccount).connection.blockUser

    @Provides
    fun provideUnblockUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UnblockUserUseCase =
        coreLogic.getSessionScope(currentAccount).connection.unblockUser

    @Provides
    fun provideObserveValidAccountsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveValidAccountsUseCase =
        coreLogic.getGlobalScope().observeValidAccounts

    @Provides
    fun provideDoesValidSessionExistsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DoesValidSessionExistUseCase =
        coreLogic.getGlobalScope().doesValidSessionExist

    @Provides
    fun provideDoesValidNomadAccountExistUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DoesValidNomadAccountExistUseCase =
        coreLogic.getGlobalScope().doesValidNomadAccountExist

    @Provides
    fun observeSecurityClassificationLabelUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSecurityClassificationLabelUseCase =
        coreLogic.getSessionScope(currentAccount).observeSecurityClassificationLabel

    @Provides
    fun provideCreateMpBackupUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): CreateMPBackupUseCase =
        coreLogic.getSessionScope(currentAccount).multiPlatformBackup.create

    @Provides
    fun provideRestoreMpBackupUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): RestoreMPBackupUseCase =
        coreLogic.getSessionScope(currentAccount).multiPlatformBackup.restore

    @Provides
    fun provideUpdateApiVersionsScheduler(@KaliumCoreLogic coreLogic: CoreLogic): UpdateApiVersionsScheduler =
        coreLogic.getGlobalScope().updateApiVersionsScheduler

    @Provides
    fun provideObserveIfAppFreshEnoughUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveIfAppUpdateRequiredUseCase =
        coreLogic.getGlobalScope().observeIfAppUpdateRequired

    @Provides
    fun provideMarkFileSharingStatusAsNotified(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkFileSharingChangeAsNotifiedUseCase = coreLogic.getSessionScope(currentAccount).markFileSharingStatusAsNotified

    @Provides
    fun provideMarkSelfDeletingMessagesAsNotified(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkSelfDeletionStatusAsNotifiedUseCase = coreLogic.getSessionScope(currentAccount).markSelfDeletingMessagesAsNotified

    @Provides
    fun provideObserveTeamSettingsSelfDeletionStatusFlagUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveTeamSettingsSelfDeletingStatusUseCase = coreLogic.getSessionScope(currentAccount).observeTeamSettingsSelfDeletionStatus

    @Provides
    fun provideObserveSelfDeletionTimerSettingsForConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSelfDeletionTimerSettingsForConversationUseCase = coreLogic.getSessionScope(currentAccount).observeSelfDeletingMessages

    @Provides
    fun providePersistNewSelfDeletingMessagesUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): PersistNewSelfDeletionTimerUseCase = coreLogic.getSessionScope(currentAccount).persistNewSelfDeletionStatus

    @Provides
    fun provideImageUtil(): ImageUtil = ImageUtil

    @Provides
    fun provideObserveGuestRoomLinkFeatureFlagUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveGuestRoomLinkFeatureFlagUseCase =
        coreLogic.getSessionScope(currentAccount).observeGuestRoomLinkFeatureFlag

    @Provides
    fun provideMarkGuestLinkFeatureFlagAsNotChangedUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkGuestLinkFeatureFlagAsNotChangedUseCase =
        coreLogic.getSessionScope(currentAccount).markGuestLinkFeatureFlagAsNotChanged

    @Provides
    fun provideMarkTeamAppLockStatusAsNotifiedUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkTeamAppLockStatusAsNotifiedUseCase =
        coreLogic.getSessionScope(currentAccount).markTeamAppLockStatusAsNotified

    @Provides
    fun provideGetOtherUserSecurityClassificationLabelUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveOtherUserSecurityClassificationLabelUseCase =
        coreLogic.getSessionScope(currentAccount).getOtherUserSecurityClassificationLabel

    @Provides
    fun provideObserveNewClientsUseCaseUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveNewClientsUseCase =
        coreLogic.getGlobalScope().observeNewClientsUseCase

    @Provides
    fun provideClearNewClientsForUser(@KaliumCoreLogic coreLogic: CoreLogic): ClearNewClientsForUserUseCase =
        coreLogic.getGlobalScope().clearNewClientsForUser

    @Provides
    fun providePersistScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): PersistScreenshotCensoringConfigUseCase = coreLogic.getSessionScope(currentAccount).persistScreenshotCensoringConfig

    @Provides
    fun provideObserveScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveScreenshotCensoringConfigUseCase = coreLogic.getSessionScope(currentAccount).observeScreenshotCensoringConfig

    @Provides
    fun provideObserveIsAppLockEditableUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic
    ): ObserveIsAppLockEditableUseCase = coreLogic.getGlobalScope().observeIsAppLockEditableUseCase

    @Provides
    fun provideObserveLegalHoldRequestUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveLegalHoldRequestUseCase =
        coreLogic.getSessionScope(currentAccount).observeLegalHoldRequest

    @Provides
    fun provideObserveLegalHoldForSelfUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveLegalHoldStateForSelfUserUseCase =
        coreLogic.getSessionScope(currentAccount).observeLegalHoldForSelfUser

    @Provides
    fun provideObserveLegalHoldForUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveLegalHoldStateForUserUseCase =
        coreLogic.getSessionScope(currentAccount).observeLegalHoldStateForUser

    @Provides
    fun provideFetchConversationMLSVerificationStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FetchConversationMLSVerificationStatusUseCase = coreLogic.getSessionScope(currentAccount).fetchConversationMLSVerificationStatus

    @Provides
    fun provideGetCurrentAnalyticsTrackingIdentifierUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetCurrentAnalyticsTrackingIdentifierUseCase = coreLogic.getSessionScope(currentAccount).getCurrentAnalyticsTrackingIdentifier

    @Provides
    fun provideMigrateFromPersonalToTeamUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MigrateFromPersonalToTeamUseCase = coreLogic.getSessionScope(currentAccount).migrateFromPersonalToTeam

    @Provides
    fun provideGetTeamUrlUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetTeamUrlUseCase = coreLogic.getSessionScope(currentAccount).getTeamUrlUseCase

    @Provides
    fun provideGenerateRandomPasswordUseCase(): RandomPassword = RandomPassword()
}
