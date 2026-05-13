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
import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import com.wire.android.BuildConfig
import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.config.ServerConfigProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.common.banner.SecurityClassificationViewModelFactory
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModelFactory
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModelFactory
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModelFactory
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModelFactory
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModelFactory
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModelFactory
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModelFactory
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModelFactory
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModelFactory
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModelFactory
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.SavedStateLoginSavedInputStore
import com.wire.android.ui.authentication.login.email.LoginEmailViewModelFactory
import com.wire.android.ui.authentication.login.sso.LoginSSOSessionExceptionClassifier
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelFactory
import com.wire.android.ui.authentication.welcome.WelcomeViewModelFactory
import com.wire.android.ui.debug.AndroidExportObfuscatedCopyFileGateway
import com.wire.android.ui.debug.AndroidDebugDataInfoProvider
import com.wire.android.ui.debug.DebugDataInfoProvider
import com.wire.android.ui.debug.DebugDataOptionsViewModelFactory
import com.wire.android.ui.debug.ExportObfuscatedCopyFileGateway
import com.wire.android.ui.debug.ExportObfuscatedCopyViewModelFactory
import com.wire.android.ui.debug.LogManagementViewModelFactory
import com.wire.android.ui.debug.UserDebugViewModelFactory
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsViewModelFactory
import com.wire.android.ui.debug.conversation.DebugConversationViewModelFactory
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModelFactory
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentViewModelFactory
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModelFactory
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModelFactory
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModelFactory
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModelFactory
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModelFactory
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversations.folder.NewFolderViewModelFactory
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewAssetImporter
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewAssetImporterImpl
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewViewModelFactory
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.gallery.MediaGalleryViewModelFactory
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
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailViewModelFactory
import com.wire.android.ui.home.settings.account.handle.ChangeHandleViewModelFactory
import com.wire.android.ui.home.settings.backup.AndroidBackupFileGateway
import com.wire.android.ui.home.settings.backup.BackupAndRestoreViewModelFactory
import com.wire.android.ui.home.settings.backup.BackupFileGateway
import com.wire.android.ui.home.settings.backup.MPBackupSettings
import com.wire.android.ui.home.settings.privacy.PrivacySettingsViewModelFactory
import com.wire.android.ui.home.whatsnew.AndroidReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.ReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.WhatsNewViewModelFactory
import com.wire.android.ui.analytics.AnalyticsConfiguration
import com.wire.android.ui.initialsync.InitialSyncViewModelFactory
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModelFactory
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModelFactory
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModelFactory
import com.wire.android.ui.settings.about.AboutThisAppInfoProvider
import com.wire.android.ui.settings.about.AboutThisAppViewModelFactory
import com.wire.android.ui.settings.about.AndroidAboutThisAppInfoProvider
import com.wire.android.ui.settings.devices.DeviceDetailsViewModelFactory
import com.wire.android.ui.settings.devices.SelfDevicesViewModelFactory
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsViewModelFactory
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModelFactory
import com.wire.android.ui.joinConversation.JoinConversationViaCodeViewModelFactory
import com.wire.android.ui.connection.ConnectionActionButtonViewModelFactory
import com.wire.android.ui.newauthentication.login.NewLoginRecoverableLogoutExceptionDetector
import com.wire.android.ui.newauthentication.login.NewLoginViewModelFactory
import com.wire.android.ui.newauthentication.login.ValidateEmailOrSSOCodeUseCase
import com.wire.android.ui.userprofile.avatarpicker.AndroidAvatarImageGateway
import com.wire.android.ui.userprofile.avatarpicker.AvatarImageGateway
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModelFactory
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModelFactory
import com.wire.android.ui.userprofile.qr.AndroidSelfQRCodeAssetRepository
import com.wire.android.ui.userprofile.qr.SelfQRCodeAssetRepository
import com.wire.android.ui.userprofile.qr.SelfQRCodeViewModelFactory
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModelFactory
import com.wire.android.ui.userprofile.service.ServiceDetailsViewModelFactory
import com.wire.android.ui.sharing.ImportMediaAssetImporter
import com.wire.android.ui.sharing.ImportMediaAssetImporterImpl
import com.wire.android.ui.sharing.ImportMediaAuthenticatedViewModelFactory
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.EMPTY
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.isWebsocketEnabledByDefault
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.android.util.logging.LogFileWriter
import com.wire.android.util.ui.AndroidUiTextResolver
import com.wire.android.util.ui.CountdownTimer
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.cells.CellsScope
import com.wire.kalium.cells.domain.usecase.GetCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetMessageAttachmentUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.conversation.FetchConversationUseCase
import com.wire.kalium.logic.data.conversation.ResetMLSConversationUseCase
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.app.AppScope
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberUseCase
import com.wire.kalium.logic.feature.backup.BackupScope
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.CreateMPBackupUseCase
import com.wire.kalium.logic.feature.backup.CreateObfuscatedCopyUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreMPBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollment
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.IsProfileQRCodeEnabledUseCase
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.ConnectionScope
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationLeaveConditionsUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationScope
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedLocallyUseCase
import com.wire.kalium.logic.feature.conversation.getPaginatedFlowOfConversationDetailsWithEventsBySearchQuery
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.CreateConversationFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.GetFavoriteFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveConversationsFromFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import com.wire.kalium.logic.feature.debug.BreakSessionUseCase
import com.wire.kalium.logic.feature.debug.ChangeProfilingUseCase
import com.wire.kalium.logic.feature.debug.DebugScope
import com.wire.kalium.logic.feature.debug.DebugFeedConversationUseCase
import com.wire.kalium.logic.feature.debug.GetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsUseCase
import com.wire.kalium.logic.feature.debug.GetConversationEpochFromCCUseCase
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import com.wire.kalium.logic.feature.debug.ObserveIsConsumableNotificationsEnabledUseCase
import com.wire.kalium.logic.feature.debug.ObserveDatabaseLoggerStateUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.SetDebugE2EICertificateExpirationUseCase
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserMlsClientIdentitiesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.team.TeamScope
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.service.ServiceScope
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.MessageScope
import com.wire.kalium.logic.feature.team.SyncSelfTeamInfoUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.UpdateAccentColorUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import com.wire.kalium.logic.feature.user.UserScope
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.ObserveTypingIndicatorEnabledUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.PersistTypingIndicatorStatusConfigUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import com.wire.kalium.util.DelicateKaliumApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.runBlocking

abstract class WireMetroScope private constructor()

@DependencyGraph(WireMetroScope::class)
@Suppress("LargeClass", "TooManyFunctions")
interface WireMetroGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides @ApplicationContext context: Context): WireMetroGraph
    }

    val checkAssetRestrictionsViewModelFactory: CheckAssetRestrictionsViewModelFactory
    val aboutThisAppViewModelFactory: AboutThisAppViewModelFactory
    val createAccountCodeViewModelFactory: CreateAccountCodeViewModelFactory
    val createAccountDetailsViewModelFactory: CreateAccountDetailsViewModelFactory
    val createAccountDataDetailViewModelFactory: CreateAccountDataDetailViewModelFactory
    val createAccountEmailViewModelFactory: CreateAccountEmailViewModelFactory
    val createAccountOverviewViewModelFactory: CreateAccountOverviewViewModelFactory
    val createAccountSummaryViewModelFactory: CreateAccountSummaryViewModelFactory
    val createAccountUsernameViewModelFactory: CreateAccountUsernameViewModelFactory
    val createAccountVerificationCodeViewModelFactory: CreateAccountVerificationCodeViewModelFactory
    val createAccountSelectorViewModelFactory: CreateAccountSelectorViewModelFactory
    val loginEmailViewModelFactory: LoginEmailViewModelFactory
    val loginSSOViewModelFactory: LoginSSOViewModelFactory
    val whatsNewViewModelFactory: WhatsNewViewModelFactory
    val dependenciesViewModelFactory: DependenciesViewModelFactory
    val licensesViewModelFactory: LicensesViewModelFactory
    val userDebugViewModelFactory: UserDebugViewModelFactory
    val conversationCryptoStatsViewModelFactory: ConversationCryptoStatsViewModelFactory
    val debugConversationViewModelFactory: DebugConversationViewModelFactory
    val logManagementViewModelFactory: LogManagementViewModelFactory
    val debugFeatureFlagsViewModelFactory: DebugFeatureFlagsViewModelFactory
    val debugDataOptionsViewModelFactory: DebugDataOptionsViewModelFactory
    val exportObfuscatedCopyViewModelFactory: ExportObfuscatedCopyViewModelFactory
    val customizationViewModelFactory: CustomizationViewModelFactory
    val initialSyncViewModelFactory: InitialSyncViewModelFactory
    val networkSettingsViewModelFactory: NetworkSettingsViewModelFactory
    val e2EIEnrollmentViewModelFactory: E2EIEnrollmentViewModelFactory
    val getE2EICertificateViewModelFactory: GetE2EICertificateViewModelFactory
    val clearSessionViewModelFactory: ClearSessionViewModelFactory
    val removeDeviceViewModelFactory: RemoveDeviceViewModelFactory
    val registerDeviceViewModelFactory: RegisterDeviceViewModelFactory
    val settingsViewModelFactory: SettingsViewModelFactory
    val selfDevicesViewModelFactory: SelfDevicesViewModelFactory
    val deviceDetailsViewModelFactory: DeviceDetailsViewModelFactory
    val e2eiCertificateDetailsViewModelFactory: E2eiCertificateDetailsViewModelFactory
    val avatarPickerViewModelFactory: AvatarPickerViewModelFactory
    val changeUserColorViewModelFactory: ChangeUserColorViewModelFactory
    val changeEmailViewModelFactory: ChangeEmailViewModelFactory
    val verifyEmailViewModelFactory: VerifyEmailViewModelFactory
    val changeDisplayNameViewModelFactory: ChangeDisplayNameViewModelFactory
    val changeHandleViewModelFactory: ChangeHandleViewModelFactory
    val myAccountViewModelFactory: MyAccountViewModelFactory
    val deleteAccountViewModelFactory: DeleteAccountViewModelFactory
    val backupAndRestoreViewModelFactory: BackupAndRestoreViewModelFactory
    val appUnlockWithBiometricsViewModelFactory: AppUnlockWithBiometricsViewModelFactory
    val enterLockScreenViewModelFactory: EnterLockScreenViewModelFactory
    val newFolderViewModelFactory: NewFolderViewModelFactory
    val forgotLockScreenViewModelFactory: ForgotLockScreenViewModelFactory
    val setLockScreenViewModelFactory: SetLockScreenViewModelFactory
    val privacySettingsViewModelFactory: PrivacySettingsViewModelFactory
    val selfQRCodeViewModelFactory: SelfQRCodeViewModelFactory
    val mediaGalleryViewModelFactory: MediaGalleryViewModelFactory
    val imagesPreviewViewModelFactory: ImagesPreviewViewModelFactory
    val otherUserProfileScreenViewModelFactory: OtherUserProfileScreenViewModelFactory
    val selfUserProfileViewModelFactory: SelfUserProfileViewModelFactory
    val serviceDetailsViewModelFactory: ServiceDetailsViewModelFactory
    val welcomeViewModelFactory: WelcomeViewModelFactory
    val newLoginViewModelFactory: NewLoginViewModelFactory
    val importMediaAuthenticatedViewModelFactory: ImportMediaAuthenticatedViewModelFactory
    val joinConversationViaCodeViewModelFactory: JoinConversationViaCodeViewModelFactory
    val securityClassificationViewModelFactory: SecurityClassificationViewModelFactory
    val connectionActionButtonViewModelFactory: ConnectionActionButtonViewModelFactory
    val conversationOptionsMenuViewModelFactory: ConversationOptionsMenuViewModelFactory

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
    fun provideCurrentServerConfig(
        managedConfigurationsManager: ManagedConfigurationsManager,
    ): ServerConfig.Links =
        if (BuildConfig.EMM_SUPPORT_ENABLED) {
            managedConfigurationsManager.currentServerConfig
        } else {
            ServerConfigProvider().getDefaultServerConfig(null)
        }

    @Named("ssoCodeConfig")
    @Provides
    fun provideCurrentSSOCodeConfig(
        managedConfigurationsManager: ManagedConfigurationsManager,
    ): String =
        if (BuildConfig.EMM_SUPPORT_ENABLED) {
            managedConfigurationsManager.currentSSOCodeConfig
        } else {
            String.EMPTY
        }

    @Provides
    fun provideLoginSavedInputStore(): LoginSavedInputStore =
        SavedStateLoginSavedInputStore(SavedStateHandle())

    @Provides
    fun provideCountdownTimer(): CountdownTimer =
        CountdownTimer()

    @Provides
    fun provideClientScopeProviderFactory(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ClientScopeProvider.Factory =
        object : ClientScopeProvider.Factory {
            override fun create(userId: UserId): ClientScopeProvider =
                ClientScopeProvider(coreLogic, userId)
        }

    @Provides
    fun provideAddAuthenticatedUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): AddAuthenticatedUserUseCase =
        coreLogic.getGlobalScope().addAuthenticatedAccount

    @DefaultWebSocketEnabledByDefault
    @Provides
    fun provideDefaultWebSocketEnabledByDefault(
        @ApplicationContext context: Context,
        managedConfigurationsManager: ManagedConfigurationsManager,
    ): Boolean =
        isWebsocketEnabledByDefault(
            context,
            managedConfigurationsManager.persistentWebSocketEnforcedByMDM.value
        )

    @Provides
    fun provideLoginSSOSessionExceptionClassifier(): LoginSSOSessionExceptionClassifier =
        LoginSSOSessionExceptionClassifier()

    @Provides
    fun provideLockCodeTimeManager(entryPoint: WireMetroHiltEntryPoint): LockCodeTimeManager =
        entryPoint.lockCodeTimeManager()

    @Provides
    fun provideWireNotificationManager(entryPoint: WireMetroHiltEntryPoint): WireNotificationManager =
        entryPoint.wireNotificationManager()

    @Provides
    fun provideLogFileWriter(entryPoint: WireMetroHiltEntryPoint): LogFileWriter =
        entryPoint.logFileWriter()

    @Provides
    fun provideAccountSwitchUseCase(entryPoint: WireMetroHiltEntryPoint): AccountSwitchUseCase =
        entryPoint.accountSwitchUseCase()

    @Provides
    fun provideAnonymousAnalyticsManager(): AnonymousAnalyticsManager =
        AnonymousAnalyticsManagerImpl

    @Provides
    fun provideOtherAccountMapper(): OtherAccountMapper =
        OtherAccountMapper()

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
    fun provideClientScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ClientScope =
        coreLogic.getSessionScope(currentAccount).client

    @Provides
    fun provideDebugScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): DebugScope =
        coreLogic.getSessionScope(currentAccount).debug

    @Provides
    fun provideConnectionScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ConnectionScope =
        coreLogic.getSessionScope(currentAccount).connection

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
    fun provideSelfQRCodeAssetRepository(
        @ApplicationContext context: Context,
        kaliumFileSystem: KaliumFileSystem,
        dispatchers: DispatcherProvider,
    ): SelfQRCodeAssetRepository =
        AndroidSelfQRCodeAssetRepository(
            context = context,
            kaliumFileSystem = kaliumFileSystem,
            dispatchers = dispatchers,
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
    fun provideDebugFeedConversationUseCase(debugScope: DebugScope): DebugFeedConversationUseCase =
        debugScope.debugFeedConversationUseCase

    @Provides
    fun provideGetConversationEpochFromCCUseCase(debugScope: DebugScope): GetConversationEpochFromCCUseCase =
        debugScope.getConversationEpochFromCC

    @Provides
    fun provideBreakSessionUseCase(debugScope: DebugScope): BreakSessionUseCase =
        debugScope.breakSession

    @Provides
    fun provideDebugDataInfoProvider(@ApplicationContext context: Context): DebugDataInfoProvider =
        AndroidDebugDataInfoProvider(context)

    @Provides
    fun provideUpdateApiVersionsScheduler(@KaliumCoreLogic coreLogic: CoreLogic): UpdateApiVersionsScheduler =
        coreLogic.getGlobalScope().updateApiVersionsScheduler

    @Provides
    fun provideMlsKeyPackageCountUseCase(clientScope: ClientScope): MLSKeyPackageCountUseCase =
        clientScope.mlsKeyPackageCountUseCase

    @Provides
    fun provideRestartSlowSyncProcessForRecoveryUseCase(clientScope: ClientScope): RestartSlowSyncProcessForRecoveryUseCase =
        clientScope.restartSlowSyncProcessForRecoveryUseCase

    @Provides
    fun provideCheckCrlRevocationListUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): CheckCrlRevocationListUseCase =
        coreLogic.getSessionScope(currentAccount).checkCrlRevocationList

    @Provides
    fun provideGetCurrentAnalyticsTrackingIdentifierUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): GetCurrentAnalyticsTrackingIdentifierUseCase =
        coreLogic.getSessionScope(currentAccount).getCurrentAnalyticsTrackingIdentifier

    @Provides
    fun provideSendFCMTokenUseCase(debugScope: DebugScope): SendFCMTokenUseCase =
        debugScope.sendFCMTokenToServer

    @Provides
    fun provideObserveIsConsumableNotificationsEnabledUseCase(debugScope: DebugScope): ObserveIsConsumableNotificationsEnabledUseCase =
        debugScope.observeIsConsumableNotificationsEnabled

    @Provides
    fun provideStartUsingAsyncNotificationsUseCase(debugScope: DebugScope): StartUsingAsyncNotificationsUseCase =
        debugScope.startUsingAsyncNotifications

    @Provides
    fun provideRepairFaultyRemovalKeysUseCase(debugScope: DebugScope): RepairFaultyRemovalKeysUseCase =
        debugScope.repairFaultyRemovalKeysUseCase

    @Provides
    fun provideGetDebugE2EICertificateExpirationUseCase(debugScope: DebugScope): GetDebugE2EICertificateExpirationUseCase =
        debugScope.getDebugE2EICertificateExpiration

    @Provides
    fun provideSetDebugE2EICertificateExpirationUseCase(debugScope: DebugScope): SetDebugE2EICertificateExpirationUseCase =
        debugScope.setDebugE2EICertificateExpiration

    @Provides
    fun provideChangeProfilingUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ChangeProfilingUseCase =
        coreLogic.getSessionScope(currentAccount).debug.changeProfiling

    @Provides
    fun provideObserveDatabaseLoggerStateUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveDatabaseLoggerStateUseCase =
        coreLogic.getSessionScope(currentAccount).debug.observeDatabaseLoggerState

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
    fun provideObserveUserInfoUseCase(userScope: UserScope): ObserveUserInfoUseCase =
        userScope.observeUserInfo

    @Provides
    fun provideUpdateSelfAvailabilityStatusUseCase(userScope: UserScope): UpdateSelfAvailabilityStatusUseCase =
        userScope.updateSelfAvailabilityStatus

    @Provides
    fun provideCanMigrateFromPersonalToTeamUseCase(userScope: UserScope): CanMigrateFromPersonalToTeamUseCase =
        userScope.isPersonalToTeamAccountSupportedByBackend

    @Provides
    fun provideIsProfileQRCodeEnabledUseCase(userScope: UserScope): IsProfileQRCodeEnabledUseCase =
        userScope.isProfileQRCodeEnabled

    @Provides
    fun provideObserveValidAccountsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveValidAccountsUseCase =
        coreLogic.getGlobalScope().observeValidAccounts

    @Provides
    fun provideObserveLegalHoldStateForSelfUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveLegalHoldStateForSelfUserUseCase =
        coreLogic.getSessionScope(currentAccount).observeLegalHoldForSelfUser

    @Provides
    fun provideGetTeamUrlUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): GetTeamUrlUseCase =
        coreLogic.getSessionScope(currentAccount).getTeamUrlUseCase

    @Provides
    fun provideUserTypeMapper(): UserTypeMapper =
        UserTypeMapper()

    @Provides
    fun provideGetSelfUserUseCase(userScope: UserScope): GetSelfUserUseCase =
        userScope.getSelfUser

    @Provides
    fun provideGetMLSClientIdentityUseCase(userScope: UserScope): GetMLSClientIdentityUseCase =
        userScope.getE2EICertificate

    @Provides
    fun provideIsOtherUserE2EIVerifiedUseCase(userScope: UserScope): IsOtherUserE2EIVerifiedUseCase =
        userScope.getUserE2eiCertificateStatus

    @Provides
    fun provideSelfServerConfigUseCase(userScope: UserScope): SelfServerConfigUseCase =
        userScope.serverLinks

    @Provides
    fun provideGetDefaultProtocolUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): GetDefaultProtocolUseCase =
        coreLogic.getSessionScope(currentAccount).getDefaultProtocol

    @Provides
    fun provideAnalyticsConfiguration(): AnalyticsConfiguration =
        if (BuildConfig.ANALYTICS_ENABLED) AnalyticsConfiguration.Enabled else AnalyticsConfiguration.Disabled

    @Provides
    fun provideRegistrationAnalyticsManagerUseCase(
        globalDataStore: GlobalDataStore,
        anonymousAnalyticsManager: AnonymousAnalyticsManager,
    ): RegistrationAnalyticsManagerUseCase =
        RegistrationAnalyticsManagerUseCase(
            globalDataStore = globalDataStore,
            anonymousAnalyticsManager = anonymousAnalyticsManager,
        )

    @Provides
    fun provideFinalizeRegistrationAnalyticsMetadataUseCase(
        globalDataStore: GlobalDataStore,
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): FinalizeRegistrationAnalyticsMetadataUseCase =
        FinalizeRegistrationAnalyticsMetadataUseCase(
            globalDataStore = globalDataStore,
            currentAccount = currentAccount,
            coreLogic = coreLogic,
        )

    @Provides
    fun provideObserveReadReceiptsEnabledUseCase(userScope: UserScope): ObserveReadReceiptsEnabledUseCase =
        userScope.observeReadReceiptsEnabled

    @Provides
    fun providePersistReadReceiptsStatusConfigUseCase(userScope: UserScope): PersistReadReceiptsStatusConfigUseCase =
        userScope.persistReadReceiptsStatusConfig

    @Provides
    fun provideObserveTypingIndicatorEnabledUseCase(userScope: UserScope): ObserveTypingIndicatorEnabledUseCase =
        userScope.observeTypingIndicatorEnabled

    @Provides
    fun providePersistTypingIndicatorStatusConfigUseCase(userScope: UserScope): PersistTypingIndicatorStatusConfigUseCase =
        userScope.persistTypingIndicatorStatusConfig

    @Provides
    fun provideObserveScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveScreenshotCensoringConfigUseCase =
        coreLogic.getSessionScope(currentAccount).observeScreenshotCensoringConfig

    @Provides
    fun providePersistScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): PersistScreenshotCensoringConfigUseCase =
        coreLogic.getSessionScope(currentAccount).persistScreenshotCensoringConfig

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
    fun provideValidateEmailUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ValidateEmailUseCase =
        coreLogic.getGlobalScope().validateEmailUseCase

    @Provides
    fun provideValidateSSOCodeUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ValidateSSOCodeUseCase =
        coreLogic.getGlobalScope().validateSSOCodeUseCase

    @Provides
    fun provideValidateEmailOrSSOCodeUseCase(
        validateEmail: ValidateEmailUseCase,
        validateSSOCode: ValidateSSOCodeUseCase,
    ): ValidateEmailOrSSOCodeUseCase =
        ValidateEmailOrSSOCodeUseCase(validateEmail, validateSSOCode)

    @Provides
    fun provideNewLoginRecoverableLogoutExceptionDetector(): NewLoginRecoverableLogoutExceptionDetector =
        NewLoginRecoverableLogoutExceptionDetector()

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
    fun provideDeleteClientUseCase(clientScope: ClientScope): DeleteClientUseCase =
        clientScope.deleteClient

    @Provides
    fun provideGetOrRegisterClientUseCase(clientScope: ClientScope): GetOrRegisterClientUseCase =
        clientScope.getOrRegister

    @Provides
    fun provideFetchSelfClientsFromRemoteUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): FetchSelfClientsFromRemoteUseCase =
        coreLogic.getSessionScope(currentAccount).client.fetchSelfClients

    @Provides
    fun provideFetchUsersClientsFromRemoteUseCase(clientScope: ClientScope): FetchUsersClientsFromRemoteUseCase =
        clientScope.fetchUsersClients

    @Provides
    fun provideClientFingerPrintUseCase(clientScope: ClientScope): ClientFingerprintUseCase =
        clientScope.remoteClientFingerPrint

    @Provides
    fun provideUpdateClientVerificationStatusUseCase(clientScope: ClientScope): UpdateClientVerificationStatusUseCase =
        clientScope.updateClientVerificationStatus

    @Provides
    fun provideObserveClientDetailsUseCase(clientScope: ClientScope): ObserveClientDetailsUseCase =
        clientScope.observeClientDetailsUseCase

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
    fun provideSyncSelfTeamInfoUseCase(teamScope: TeamScope): SyncSelfTeamInfoUseCase =
        teamScope.syncSelfTeamInfoUseCase

    @Provides
    fun provideConversationScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ConversationScope =
        coreLogic.getSessionScope(currentAccount).conversations

    @Provides
    fun provideObserveConversationDetailsUseCase(conversationScope: ConversationScope): ObserveConversationDetailsUseCase =
        conversationScope.observeConversationDetails

    @Provides
    fun provideResetMLSConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ResetMLSConversationUseCase =
        coreLogic.getSessionScope(currentAccount).resetMlsConversation

    @Provides
    fun provideFetchConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): FetchConversationUseCase =
        coreLogic.getSessionScope(currentAccount).fetchConversationUseCase

    @Provides
    fun provideObserveUserFoldersUseCase(conversationScope: ConversationScope): ObserveUserFoldersUseCase =
        conversationScope.observeUserFolders

    @Provides
    fun provideCreateConversationFolderUseCase(conversationScope: ConversationScope): CreateConversationFolderUseCase =
        conversationScope.createConversationFolder

    @Provides
    fun provideObserveConversationMembersUseCase(conversationScope: ConversationScope): ObserveConversationMembersUseCase =
        conversationScope.observeConversationMembers

    @Provides
    fun provideObserveConversationRoleForUserUseCase(
        observeConversationMembers: ObserveConversationMembersUseCase,
        observeConversationDetails: ObserveConversationDetailsUseCase,
        observeSelfUser: ObserveSelfUserUseCase,
    ): ObserveConversationRoleForUserUseCase =
        ObserveConversationRoleForUserUseCase(
            observeConversationMembers = observeConversationMembers,
            observeConversationDetails = observeConversationDetails,
            observeSelfUser = observeSelfUser,
        )

    @Provides
    fun provideRemoveMemberFromConversationUseCase(conversationScope: ConversationScope): RemoveMemberFromConversationUseCase =
        conversationScope.removeMemberFromConversation

    @Provides
    fun provideUpdateConversationMemberRoleUseCase(conversationScope: ConversationScope): UpdateConversationMemberRoleUseCase =
        conversationScope.updateConversationMemberRole

    @Provides
    fun provideIsOneToOneConversationCreatedUseCase(conversationScope: ConversationScope): IsOneToOneConversationCreatedUseCase =
        conversationScope.isOneToOneConversationCreatedUseCase

    @Provides
    fun provideAddServiceToConversationUseCase(conversationScope: ConversationScope): AddServiceToConversationUseCase =
        conversationScope.addServiceToConversationUseCase

    @Provides
    fun provideAddMemberToConversationUseCase(conversationScope: ConversationScope): AddMemberToConversationUseCase =
        conversationScope.addMemberToConversationUseCase

    @Provides
    fun provideJoinConversationViaCodeUseCase(conversationScope: ConversationScope): JoinConversationViaCodeUseCase =
        conversationScope.joinConversationViaCode

    @Provides
    fun provideGetOrCreateOneToOneConversationUseCase(conversationScope: ConversationScope): GetOrCreateOneToOneConversationUseCase =
        conversationScope.getOrCreateOneToOneConversationUseCase

    @Provides
    fun provideUpdateConversationArchivedStatusUseCase(conversationScope: ConversationScope): UpdateConversationArchivedStatusUseCase =
        conversationScope.updateConversationArchivedStatus

    @Provides
    fun provideUpdateConversationMutedStatusUseCase(conversationScope: ConversationScope): UpdateConversationMutedStatusUseCase =
        conversationScope.updateConversationMutedStatus

    @Provides
    fun provideDeleteTeamConversationUseCase(conversationScope: ConversationScope): DeleteTeamConversationUseCase =
        conversationScope.deleteTeamConversation

    @Provides
    fun provideMarkConversationAsDeletedLocallyUseCase(conversationScope: ConversationScope): MarkConversationAsDeletedLocallyUseCase =
        conversationScope.markConversationAsDeletedLocallyUseCase

    @Provides
    fun provideLeaveConversationUseCase(conversationScope: ConversationScope): LeaveConversationUseCase =
        conversationScope.leaveConversation

    @Provides
    fun provideCheckConversationLeaveConditionsUseCase(conversationScope: ConversationScope): CheckConversationLeaveConditionsUseCase =
        conversationScope.checkConversationLeaveConditions

    @Provides
    fun provideClearConversationContentUseCase(conversationScope: ConversationScope): ClearConversationContentUseCase =
        conversationScope.clearConversationContent

    @Provides
    fun provideSendConnectionRequestUseCase(connectionScope: ConnectionScope): SendConnectionRequestUseCase =
        connectionScope.sendConnectionRequest

    @Provides
    fun provideCancelConnectionRequestUseCase(connectionScope: ConnectionScope): CancelConnectionRequestUseCase =
        connectionScope.cancelConnectionRequest

    @Provides
    fun provideIgnoreConnectionRequestUseCase(connectionScope: ConnectionScope): IgnoreConnectionRequestUseCase =
        connectionScope.ignoreConnectionRequest

    @Provides
    fun provideAcceptConnectionRequestUseCase(connectionScope: ConnectionScope): AcceptConnectionRequestUseCase =
        connectionScope.acceptConnectionRequest

    @Provides
    fun provideBlockUserUseCase(connectionScope: ConnectionScope): BlockUserUseCase =
        connectionScope.blockUser

    @Provides
    fun provideUnblockUserUseCase(connectionScope: ConnectionScope): UnblockUserUseCase =
        connectionScope.unblockUser

    @Provides
    fun provideAppScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): AppScope =
        coreLogic.getSessionScope(currentAccount).apps

    @Provides
    fun provideGetAppByIdUseCase(appScope: AppScope): GetAppByIdUseCase =
        appScope.getAppById

    @Provides
    fun provideObserveIsAppMemberUseCase(appScope: AppScope): ObserveIsAppMemberUseCase =
        appScope.observeIsAppMember

    @Provides
    fun provideServiceScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ServiceScope =
        coreLogic.getSessionScope(currentAccount).service

    @Provides
    fun provideGetServiceByIdUseCase(serviceScope: ServiceScope): GetServiceByIdUseCase =
        serviceScope.getServiceById

    @Provides
    fun provideObserveIsServiceMemberUseCase(serviceScope: ServiceScope): ObserveIsServiceMemberUseCase =
        serviceScope.observeIsServiceMember

    @Provides
    fun provideObserveIsAppsAllowedForUsageUseCase(serviceScope: ServiceScope): ObserveIsAppsAllowedForUsageUseCase =
        serviceScope.observeIsAppsAllowedForUsage

    @Provides
    fun provideMessageScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): MessageScope =
        coreLogic.getSessionScope(currentAccount).messages

    @Provides
    fun provideGetMessageAssetUseCase(messageScope: MessageScope): GetMessageAssetUseCase =
        messageScope.getAssetMessage

    @Provides
    fun provideDeleteMessageUseCase(messageScope: MessageScope): DeleteMessageUseCase =
        messageScope.deleteMessage

    @Provides
    fun provideCellsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): CellsScope =
        coreLogic.getSessionScope(currentAccount).cells

    @Provides
    fun provideGetMessageAttachmentUseCase(cellsScope: CellsScope): GetMessageAttachmentUseCase =
        cellsScope.getMessageAttachmentUseCase

    @Provides
    fun provideGetCellFileUseCase(cellsScope: CellsScope): GetCellFileUseCase =
        cellsScope.getCellFileUseCase

    @Provides
    fun provideFileManager(@ApplicationContext context: Context): FileManager =
        FileManager(context)

    @Provides
    fun provideUiTextResolver(@ApplicationContext context: Context): UiTextResolver =
        AndroidUiTextResolver(context)

    @Provides
    fun provideGetAssetSizeLimitUseCase(userScope: UserScope): GetAssetSizeLimitUseCase =
        userScope.getAssetSizeLimit

    @Provides
    fun provideHandleUriAssetUseCase(
        getAssetSizeLimitUseCase: GetAssetSizeLimitUseCase,
        fileManager: FileManager,
        kaliumFileSystem: KaliumFileSystem,
        dispatchers: DispatcherProvider,
    ): HandleUriAssetUseCase =
        HandleUriAssetUseCase(
            getAssetSizeLimit = getAssetSizeLimitUseCase,
            fileManager = fileManager,
            kaliumFileSystem = kaliumFileSystem,
            dispatchers = dispatchers,
        )

    @Provides
    fun provideImagesPreviewAssetImporter(
        handleUriAssetUseCase: HandleUriAssetUseCase,
        dispatchers: DispatcherProvider,
    ): ImagesPreviewAssetImporter =
        ImagesPreviewAssetImporterImpl(
            handleUriAsset = handleUriAssetUseCase,
            dispatchers = dispatchers,
        )

    @Provides
    fun provideImportMediaAssetImporter(
        handleUriAssetUseCase: HandleUriAssetUseCase,
        dispatchers: DispatcherProvider,
    ): ImportMediaAssetImporter =
        ImportMediaAssetImporterImpl(
            handleUriAsset = handleUriAssetUseCase,
            dispatchers = dispatchers,
        )

    @Provides
    fun provideGetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase(
        conversationScope: ConversationScope,
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
    fun provideRemoveConversationFromFavoritesUseCase(conversationScope: ConversationScope): RemoveConversationFromFavoritesUseCase =
        conversationScope.removeConversationFromFavorites

    @Provides
    fun provideRemoveConversationFromFolderUseCase(conversationScope: ConversationScope): RemoveConversationFromFolderUseCase =
        conversationScope.removeConversationFromFolder

    @Provides
    @Suppress("LongParameterList")
    fun provideGetConversationsFromSearchUseCase(
        useCase: GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase,
        getFavoriteFolderUseCase: GetFavoriteFolderUseCase,
        observeConversationsFromFolder: ObserveConversationsFromFolderUseCase,
        userTypeMapper: UserTypeMapper,
        dispatchers: DispatcherProvider,
        getSelfUser: GetSelfUserUseCase,
        uiTextResolver: UiTextResolver,
    ): GetConversationsFromSearchUseCase =
        GetConversationsFromSearchUseCase(
            useCase = useCase,
            getFavoriteFolderUseCase = getFavoriteFolderUseCase,
            observeConversationsFromFromFolder = observeConversationsFromFolder,
            userTypeMapper = userTypeMapper,
            dispatchers = dispatchers,
            getSelfUser = getSelfUser,
            uiTextResolver = uiTextResolver,
        )

    @Provides
    fun provideObserveSelfDeletionTimerSettingsForConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveSelfDeletionTimerSettingsForConversationUseCase =
        coreLogic.getSessionScope(currentAccount).observeSelfDeletingMessages

    @Provides
    fun providePersistNewSelfDeletingMessagesUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): PersistNewSelfDeletionTimerUseCase =
        coreLogic.getSessionScope(currentAccount).persistNewSelfDeletionStatus

    @Provides
    fun provideBackupScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): BackupScope =
        coreLogic.getSessionScope(currentAccount).backup

    @Provides
    fun provideCreateBackupUseCase(backupScope: BackupScope): CreateBackupUseCase =
        backupScope.create

    @OptIn(DelicateKaliumApi::class)
    @Provides
    fun provideCreateObfuscatedCopyUseCase(backupScope: BackupScope): CreateObfuscatedCopyUseCase =
        backupScope.createUnEncryptedCopy

    @Provides
    fun provideVerifyBackupUseCase(backupScope: BackupScope): VerifyBackupUseCase =
        backupScope.verify

    @Provides
    fun provideRestoreBackupUseCase(backupScope: BackupScope): RestoreBackupUseCase =
        backupScope.restore

    @Provides
    fun provideCreateMpBackupUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): CreateMPBackupUseCase =
        coreLogic.getSessionScope(currentAccount).multiPlatformBackup.create

    @Provides
    fun provideRestoreMpBackupUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): RestoreMPBackupUseCase =
        coreLogic.getSessionScope(currentAccount).multiPlatformBackup.restore

    @Provides
    fun provideMpBackupSettings(): MPBackupSettings =
        if (BuildConfig.ENABLE_CROSSPLATFORM_BACKUP) {
            MPBackupSettings.Enabled
        } else {
            MPBackupSettings.Disabled
        }

    @Provides
    fun provideBackupFileGateway(
        fileManager: FileManager,
        kaliumFileSystem: KaliumFileSystem,
        dispatchers: DispatcherProvider,
    ): BackupFileGateway =
        AndroidBackupFileGateway(
            fileManager = fileManager,
            kaliumFileSystem = kaliumFileSystem,
            dispatcher = dispatchers,
        )

    @Provides
    fun provideExportObfuscatedCopyFileGateway(
        fileManager: FileManager,
        dispatchers: DispatcherProvider,
    ): ExportObfuscatedCopyFileGateway =
        AndroidExportObfuscatedCopyFileGateway(
            fileManager = fileManager,
            dispatcher = dispatchers,
        )

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    fun provideGetSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): GetSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessions

    @Provides
    fun provideDoesValidNomadAccountExistUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DoesValidNomadAccountExistUseCase =
        coreLogic.getGlobalScope().doesValidNomadAccountExist

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
    fun provideRequestSecondFactorVerificationCodeUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): RequestSecondFactorVerificationCodeUseCase =
        coreLogic.getSessionScope(currentAccount).authenticationScope.requestSecondFactorVerificationCode

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

    fun logFileWriter(): LogFileWriter

    fun accountSwitchUseCase(): AccountSwitchUseCase
}
