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
import android.location.Geocoder
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.wire.android.BuildConfig
import com.wire.android.GlobalObserversManager
import com.wire.android.config.NomadProfilesFeatureConfig
import com.wire.android.config.ServerConfigProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ApplicationScope
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.IsProfileQRCodeEnabledUseCaseProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ObserveIfE2EIRequiredDuringLoginUseCaseProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.di.ObserveSelfUserUseCaseProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.emm.ManagedConfigurationsReceiver
import com.wire.android.emm.ManagedConfigurationsReporter
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.DisableAppLockUseCase
import com.wire.android.feature.StartPersistentWebsocketIfNecessaryUseCase
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.cells.ui.AndroidCellFileExternalActions
import com.wire.android.feature.cells.ui.CellFileExternalActions
import com.wire.android.media.audiomessage.AudioFocusHelper
import com.wire.android.media.audiomessage.AudioMessageViewModelFactory
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.media.CallRinger
import com.wire.android.media.PingRinger
import com.wire.android.mapper.MessageContentMapper
import com.wire.android.mapper.MessageMapper
import com.wire.android.mapper.MessageResourceProvider
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.mapper.ContactMapper
import com.wire.android.mapper.RegularMessageMapper
import com.wire.android.mapper.SystemMessageContentMapper
import com.wire.android.mapper.UIAssetMapper
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAssetViewModelFactory
import com.wire.android.model.ImageAssetViewModelGraph
import com.wire.android.notification.WireNotificationManager
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.broadcastreceivers.DynamicReceiversManager
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.services.CallService
import com.wire.android.services.CallServiceManager
import com.wire.android.services.PersistentWebSocketService
import com.wire.android.services.PlayingAudioMessageService
import com.wire.android.services.ServicesManager
import com.wire.android.sync.MonitorSyncWorkUseCase
import com.wire.android.ui.AndroidWireActivityIntentGateway
import com.wire.android.ui.CallFeedbackViewModelFactory
import com.wire.android.ui.WireActivityViewModelFactory
import com.wire.android.ui.WireActivityIntentGateway
import com.wire.android.ui.calling.CallActivityViewModelFactory
import com.wire.android.ui.calling.common.ProximitySensorManager
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
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModelFactory
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModelFactory
import com.wire.android.ui.home.appLock.CurrentTimestampProvider
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModelFactory
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModelFactory
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModelFactory
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModelFactory
import com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModelFactory
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModelFactory
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModelFactory
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModelFactory
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModelFactory
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentAssetImporter
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentAssetImporterImpl
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentFileGateway
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentFileGatewayImpl
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModelFactory
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModelFactory
import com.wire.android.ui.home.conversations.call.ConversationCallViewModelFactory
import com.wire.android.ui.home.conversations.composer.AndroidTempWritableAttachmentUriProvider
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModelFactory
import com.wire.android.ui.home.conversations.composer.TempWritableAttachmentUriProvider
import com.wire.android.ui.home.conversations.folder.ConversationFoldersViewModelFactory
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderViewModelFactory
import com.wire.android.ui.home.conversations.folder.NewFolderViewModelFactory
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModelFactory
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModelFactory
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModelFactory
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuViewModelFactory
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModelFactory
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewAssetImporter
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewAssetImporterImpl
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewViewModelFactory
import com.wire.android.ui.home.conversations.media.ConversationAssetMessagesViewModelFactory
import com.wire.android.ui.home.conversations.CompositeMessageViewModelFactory
import com.wire.android.ui.home.conversations.messages.AndroidConversationAssetFileGateway
import com.wire.android.ui.home.conversations.messages.ConversationAssetFileGateway
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModelFactory
import com.wire.android.ui.home.conversations.messages.QuotedMultipartMessageViewModelFactory
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathViewModelFactory
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelFactory
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModelFactory
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.CellAssetRefreshHelper
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModelFactory
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModelFactory
import com.wire.android.ui.home.conversations.search.SearchUserViewModelFactory
import com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModelFactory
import com.wire.android.ui.home.conversations.search.apps.SearchAppsViewModelFactory
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesViewModelFactory
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModelFactory
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModelFactory
import com.wire.android.ui.home.AppSyncViewModelFactory
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelFactory
import com.wire.android.ui.home.conversationslist.ConversationListViewModelFactory
import com.wire.android.ui.home.gallery.MediaGalleryViewModelFactory
import com.wire.android.ui.home.HomeViewModelFactory
import com.wire.android.ui.home.drawer.HomeDrawerViewModelFactory
import com.wire.android.ui.home.newconversation.NewConversationViewModelFactory
import com.wire.android.ui.home.messagecomposer.attachments.IsFileSharingEnabledViewModelFactory
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelFactory
import com.wire.android.ui.home.messagecomposer.location.LocationPickerParameters
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModelFactory
import com.wire.android.ui.home.messagecomposer.recordaudio.AndroidRecordAudioFileGateway
import com.wire.android.ui.home.messagecomposer.recordaudio.AudioMediaRecorder
import com.wire.android.ui.home.messagecomposer.recordaudio.GenerateAudioFileWithEffectsUseCase
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioFileGateway
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioViewModelFactory
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
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModelFactory
import com.wire.android.ui.analytics.AnalyticsConfiguration
import com.wire.android.ui.analytics.IsAnalyticsAvailableUseCase
import com.wire.android.ui.analytics.AnalyticsUsageViewModelFactory
import com.wire.android.feature.cells.ui.CellViewModelGraph
import com.wire.android.feature.cells.ui.CellViewModelFactory
import com.wire.android.feature.cells.ui.create.file.CreateFileViewModelFactory
import com.wire.android.feature.cells.ui.create.folder.CreateFolderViewModelFactory
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderViewModelFactory
import com.wire.android.feature.cells.ui.publiclink.PublicLinkViewModelFactory
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationScreenViewModelFactory
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordScreenViewModelFactory
import com.wire.android.feature.cells.ui.rename.RenameNodeViewModelFactory
import com.wire.android.feature.cells.ui.search.SearchScreenViewModelFactory
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsViewModelFactory
import com.wire.android.feature.cells.ui.versioning.VersionHistoryViewModelFactory
import com.wire.android.feature.meetings.ui.MeetingViewModelGraph
import com.wire.android.feature.meetings.ui.list.MeetingListViewModelFactory
import com.wire.android.feature.meetings.ui.options.MeetingOptionsMenuViewModelFactory
import com.wire.android.ui.calling.common.SharedCallingViewModelFactory
import com.wire.android.ui.calling.incoming.IncomingCallViewModelFactory
import com.wire.android.ui.calling.ongoing.OngoingCallViewModelFactory
import com.wire.android.ui.calling.outgoing.OutgoingCallViewModelFactory
import com.wire.android.ui.calling.usecase.HangUpCallUseCase
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
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedViewModelFactory
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModelFactory
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
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModelFactory
import com.wire.android.ui.sharing.ImportMediaAssetImporter
import com.wire.android.ui.sharing.ImportMediaAssetImporterImpl
import com.wire.android.ui.sharing.ImportMediaAuthenticatedViewModelFactory
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.EMPTY
import com.wire.android.util.FileManager
import com.wire.android.util.FileSizeFormatter
import com.wire.android.util.GetMediaMetadataUseCase
import com.wire.android.util.GetMediaMetadataUseCaseImpl
import com.wire.android.util.ImageUtil
import com.wire.android.util.NetworkUtil
import com.wire.android.util.ScreenStateObserver
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.isWebsocketEnabledByDefault
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.android.util.lifecycle.IntentsProcessor
import com.wire.android.util.lifecycle.NomadIntentSignatureValidator
import com.wire.android.util.lifecycle.SyncLifecycleManager
import com.wire.android.util.logging.LogFileWriter
import com.wire.android.util.logging.LogFileWriterV1Impl
import com.wire.android.util.logging.LogFileWriterV2Impl
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.time.TimeZoneProvider
import com.wire.android.util.ui.AndroidUiTextResolver
import com.wire.android.util.ui.CountdownTimer
import com.wire.android.util.ui.UiTextResolver
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.android.workmanager.WireWorkerFactory
import com.wire.kalium.cells.CellsScope
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.GetCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetMessageAttachmentUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedCellConversationsFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RefreshCellAssetStateUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RemoveNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import com.wire.kalium.cells.domain.usecase.UpdateNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateDocumentFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateFolderUseCase
import com.wire.kalium.cells.domain.usecase.create.CreatePresentationFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateSpreadsheetFileUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellVersionUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.DeletePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.SetPublicLinkExpirationUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.UpdatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.cells.domain.usecase.versioning.RestoreNodeVersionUseCase
import com.wire.kalium.cells.paginatedConversationsFlowUseCase
import com.wire.kalium.cells.paginatedFilesFlowUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.conversation.FetchConversationUseCase
import com.wire.kalium.logic.data.conversation.ResetMLSConversationUseCase
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetPaginatedFlowOfAssetMessageByConversationIdUseCase
import com.wire.kalium.logic.feature.asset.ObserveAssetStatusesUseCase
import com.wire.kalium.logic.feature.asset.ObservePaginatedAssetImageMessages
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageTransferStatusUseCase
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.app.AppScope
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.app.ObserveAllAppsUseCase
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberUseCase
import com.wire.kalium.logic.feature.app.SearchAppsByNameUseCase
import com.wire.kalium.logic.feature.backup.BackupScope
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.CreateMPBackupUseCase
import com.wire.kalium.logic.feature.backup.CreateObfuscatedCopyUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreMPBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallModerationActionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallQualityDataUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveConferenceCallingEnabledUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.SetCallQualityIntervalUseCase
import com.wire.kalium.logic.feature.call.usecase.SetUIRotationUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.channels.ChannelsScope
import com.wire.kalium.logic.feature.channels.ObserveChannelsCreationPermissionUseCase
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.ClientScope
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollment
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.IsProfileQRCodeEnabledUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledForConversationUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUsersTypingUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.ConnectionScope
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationLeaveConditionsUseCase
import com.wire.kalium.logic.feature.conversation.ClearUsersTypingEventsUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationScope
import com.wire.kalium.logic.feature.conversation.GetConversationUnreadEventsCountUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.MarkConversationAsReadLocallyUseCase
import com.wire.kalium.logic.feature.conversation.NotifyConversationIsOpenUseCase
import com.wire.kalium.logic.feature.conversation.MembersToMentionUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationInteractionAvailabilityUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.SetNotifiedAboutConversationUnderLegalHoldUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationCodeUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReadDateUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.conversation.channel.UpdateChannelAddPermissionUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateChannelUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateRegularGroupUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedLocallyUseCase
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.conversation.getPaginatedFlowOfConversationDetailsWithEventsBySearchQuery
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
import com.wire.kalium.logic.feature.e2ei.usecase.FetchConversationMLSVerificationStatusUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMembersE2EICertificateStatusesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserMlsClientIdentitiesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
import com.wire.kalium.logic.feature.team.TeamScope
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveAllServicesUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.service.SearchServicesByNameUseCase
import com.wire.kalium.logic.feature.service.ServiceScope
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.FetchOlderNomadMessagesByConversationUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesByConversationUseCase
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase
import com.wire.kalium.logic.feature.message.GetSearchedConversationMessagePositionUseCase
import com.wire.kalium.logic.feature.message.MessageScope
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageReactionsUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageReceiptsUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendLocationUseCase
import com.wire.kalium.logic.feature.message.SendMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import com.wire.kalium.logic.feature.message.draft.GetMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.RemoveMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.SaveMessageDraftUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.message.fetchOlderMessagesByConversationId
import com.wire.kalium.logic.feature.message.getPaginatedFlowOfAssetMessageByConversationId
import com.wire.kalium.logic.feature.message.getPaginatedFlowOfMessagesByConversation
import com.wire.kalium.logic.feature.message.getPaginatedFlowOfMessagesBySearchQueryAndConversation
import com.wire.kalium.logic.feature.message.observePaginatedImageAssetMessageByConversationId
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.search.IsFederationSearchAllowedUseCase
import com.wire.kalium.logic.feature.search.SearchByHandleUseCase
import com.wire.kalium.logic.feature.search.SearchScope
import com.wire.kalium.logic.feature.search.SearchUsersUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import com.wire.kalium.logic.feature.team.SyncSelfTeamInfoUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
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
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import com.wire.kalium.logic.featureFlags.BuildFileRestrictionState
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.logic.sync.ForegroundActionsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import com.wire.kalium.logic.util.RandomPassword
import com.wire.kalium.network.NetworkStateObserver
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

abstract class WireMetroScope private constructor()

@DependencyGraph(WireMetroScope::class)
@Suppress("LargeClass", "TooManyFunctions")
interface WireMetroGraph : CellViewModelGraph, MeetingViewModelGraph, ImageAssetViewModelGraph {
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
    val isFileSharingEnabledViewModelFactory: IsFileSharingEnabledViewModelFactory
    val addMembersToConversationViewModelFactory: AddMembersToConversationViewModelFactory
    val editConversationMetadataViewModelFactory: EditConversationMetadataViewModelFactory
    val searchAppsViewModelFactory: SearchAppsViewModelFactory
    val recordAudioViewModelFactory: RecordAudioViewModelFactory
    val updateAppsAccessViewModelFactory: UpdateAppsAccessViewModelFactory
    val updateChannelAccessViewModelFactory: UpdateChannelAccessViewModelFactory
    val editSelfDeletingMessagesViewModelFactory: EditSelfDeletingMessagesViewModelFactory
    val editGuestAccessViewModelFactory: EditGuestAccessViewModelFactory
    val createPasswordGuestLinkViewModelFactory: CreatePasswordGuestLinkViewModelFactory
    val searchConversationMessagesViewModelFactory: SearchConversationMessagesViewModelFactory
    val groupConversationParticipantsViewModelFactory: GroupConversationParticipantsViewModelFactory
    val messageDetailsViewModelFactory: MessageDetailsViewModelFactory
    val searchUserViewModelFactory: SearchUserViewModelFactory
    val conversationFoldersViewModelFactory: ConversationFoldersViewModelFactory
    val moveConversationToFolderViewModelFactory: MoveConversationToFolderViewModelFactory
    val typingIndicatorViewModelFactory: TypingIndicatorViewModelFactory
    val locationPickerViewModelFactory: LocationPickerViewModelFactory
    val selfDeletingMessageActionViewModelFactory: SelfDeletingMessageActionViewModelFactory
    val conversationAssetPathsViewModelFactory: ConversationAssetPathsViewModelFactory
    val multipartAttachmentsViewModelFactory: MultipartAttachmentsViewModelFactory
    val quotedMultipartMessageViewModelFactory: QuotedMultipartMessageViewModelFactory
    val messageOptionsMenuViewModelFactory: MessageOptionsMenuViewModelFactory
    val groupConversationDetailsViewModelFactory: GroupConversationDetailsViewModelFactory
    val compositeMessageViewModelFactory: CompositeMessageViewModelFactory
    val assetLocalPathViewModelFactory: AssetLocalPathViewModelFactory
    val conversationAssetMessagesViewModelFactory: ConversationAssetMessagesViewModelFactory
    val conversationMessagesViewModelFactory: ConversationMessagesViewModelFactory
    val audioMessageViewModelFactory: AudioMessageViewModelFactory
    val conversationInfoViewModelFactory: ConversationInfoViewModelFactory
    val conversationBannerViewModelFactory: ConversationBannerViewModelFactory
    val conversationCallViewModelFactory: ConversationCallViewModelFactory
    val messageComposerViewModelFactory: MessageComposerViewModelFactory
    val sendMessageViewModelFactory: SendMessageViewModelFactory
    val conversationMigrationViewModelFactory: ConversationMigrationViewModelFactory
    val messageDraftViewModelFactory: MessageDraftViewModelFactory
    val messageAttachmentsViewModelFactory: MessageAttachmentsViewModelFactory
    val homeViewModelFactory: HomeViewModelFactory
    val appSyncViewModelFactory: AppSyncViewModelFactory
    val homeDrawerViewModelFactory: HomeDrawerViewModelFactory
    val newConversationViewModelFactory: NewConversationViewModelFactory
    val analyticsUsageViewModelFactory: AnalyticsUsageViewModelFactory
    override val cellViewModelFactory: CellViewModelFactory
    override val createFileViewModelFactory: CreateFileViewModelFactory
    override val createFolderViewModelFactory: CreateFolderViewModelFactory
    override val renameNodeViewModelFactory: RenameNodeViewModelFactory
    override val moveToFolderViewModelFactory: MoveToFolderViewModelFactory
    override val addRemoveTagsViewModelFactory: AddRemoveTagsViewModelFactory
    override val versionHistoryViewModelFactory: VersionHistoryViewModelFactory
    override val publicLinkViewModelFactory: PublicLinkViewModelFactory
    override val publicLinkExpirationScreenViewModelFactory: PublicLinkExpirationScreenViewModelFactory
    override val publicLinkPasswordScreenViewModelFactory: PublicLinkPasswordScreenViewModelFactory
    override val searchScreenViewModelFactory: SearchScreenViewModelFactory
    val teamMigrationViewModelFactory: TeamMigrationViewModelFactory
    val incomingCallViewModelFactory: IncomingCallViewModelFactory
    val outgoingCallViewModelFactory: OutgoingCallViewModelFactory
    val ongoingCallViewModelFactory: OngoingCallViewModelFactory
    val sharedCallingViewModelFactory: SharedCallingViewModelFactory
    val conversationListViewModelFactory: ConversationListViewModelFactory
    val conversationListCallViewModelFactory: ConversationListCallViewModelFactory
    val commonTopAppBarViewModelFactory: CommonTopAppBarViewModelFactory
    val featureFlagNotificationViewModelFactory: FeatureFlagNotificationViewModelFactory
    val legalHoldRequestedViewModelFactory: LegalHoldRequestedViewModelFactory
    val legalHoldDeactivatedViewModelFactory: LegalHoldDeactivatedViewModelFactory
    val wireActivityViewModelFactory: WireActivityViewModelFactory
    val callFeedbackViewModelFactory: CallFeedbackViewModelFactory
    val callActivityViewModelFactory: CallActivityViewModelFactory
    override val meetingListViewModelFactory: MeetingListViewModelFactory
    override val meetingOptionsMenuViewModelFactory: MeetingOptionsMenuViewModelFactory
    override val imageAssetViewModelFactory: ImageAssetViewModelFactory

    val currentScreenManager: CurrentScreenManager
    val lockCodeTimeManager: LockCodeTimeManager
    val switchAccountObserver: SwitchAccountObserver
    val loginTypeSelector: LoginTypeSelector
    val dynamicReceiversManager: DynamicReceiversManager
    val managedConfigurationsManager: ManagedConfigurationsManager
    val proximitySensorManager: ProximitySensorManager
    val servicesManager: ServicesManager
    val callNotificationManager: CallNotificationManager
    val conversationAudioMessagePlayer: ConversationAudioMessagePlayer

    val dispatcherProvider: DispatcherProvider

    @get:ApplicationScope
    val applicationScope: CoroutineScope

    @get:KaliumCoreLogic
    val coreLogic: CoreLogic

    val networkUtil: NetworkUtil
    val persistentWebSocketServiceDependencies: PersistentWebSocketService.Dependencies
    val callServiceDependencies: CallService.Dependencies
    val playingAudioMessageServiceDependencies: PlayingAudioMessageService.Dependencies
    val currentSession: CurrentSessionUseCase
    val accountSwitch: AccountSwitchUseCase
    val nomadProfilesFeatureConfig: NomadProfilesFeatureConfig
    val startPersistentWebsocketIfNecessary: StartPersistentWebsocketIfNecessaryUseCase

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
    @KaliumCoreLogic
    fun provideKaliumCoreLogicLazy(@KaliumCoreLogic coreLogic: CoreLogic): dagger.Lazy<CoreLogic> =
        object : dagger.Lazy<CoreLogic> {
            override fun get(): CoreLogic = coreLogic
        }

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
    fun provideCurrentAccountUserDataStoreLazy(userDataStore: UserDataStore): dagger.Lazy<UserDataStore> =
        object : dagger.Lazy<UserDataStore> {
            override fun get(): UserDataStore = userDataStore
        }

    @Provides
    fun provideGlobalDataStoreLazy(globalDataStore: GlobalDataStore): dagger.Lazy<GlobalDataStore> =
        object : dagger.Lazy<GlobalDataStore> {
            override fun get(): GlobalDataStore = globalDataStore
        }

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
    fun provideCurrentTimestampProvider(): CurrentTimestampProvider =
        { System.currentTimeMillis() }

    @Provides
    fun provideGeocoder(@ApplicationContext context: Context): Geocoder =
        Geocoder(context)

    @Provides
    fun provideLocationPickerParameters(): LocationPickerParameters =
        LocationPickerParameters()

    @Provides
    fun provideLockCodeTimeManager(entryPoint: WireMetroHiltEntryPoint): LockCodeTimeManager =
        entryPoint.lockCodeTimeManager()

    @Provides
    fun provideWireNotificationManager(entryPoint: WireMetroHiltEntryPoint): WireNotificationManager =
        entryPoint.wireNotificationManager()

    @Provides
    fun provideNotificationManagerCompat(@ApplicationContext context: Context): NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    @Provides
    fun provideLogFileWriter(@ApplicationContext context: Context): LogFileWriter {
        val logsDirectory = LogFileWriter.logsDirectory(context)
        return if (BuildConfig.USE_ASYNC_FLUSH_LOGGING) {
            LogFileWriterV2Impl(logsDirectory)
        } else {
            LogFileWriterV1Impl(logsDirectory)
        }
    }

    @Provides
    fun provideAccountSwitchUseCase(entryPoint: WireMetroHiltEntryPoint): AccountSwitchUseCase =
        entryPoint.accountSwitchUseCase()

    @Provides
    fun provideAnonymousAnalyticsManager(): AnonymousAnalyticsManager =
        AnonymousAnalyticsManagerImpl

    @Provides
    fun provideAnonymousAnalyticsManagerLazy(
        anonymousAnalyticsManager: AnonymousAnalyticsManager,
    ): dagger.Lazy<AnonymousAnalyticsManager> =
        object : dagger.Lazy<AnonymousAnalyticsManager> {
            override fun get(): AnonymousAnalyticsManager = anonymousAnalyticsManager
        }

    @Provides
    fun provideLoginTypeSelector(entryPoint: WireMetroHiltEntryPoint): LoginTypeSelector =
        entryPoint.loginTypeSelector()

    @Provides
    fun provideCurrentSessionFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic): CurrentSessionFlowUseCase =
        coreLogic.getGlobalScope().session.currentSessionFlow

    @Provides
    fun provideCurrentSessionFlowUseCaseLazy(currentSessionFlow: CurrentSessionFlowUseCase): dagger.Lazy<CurrentSessionFlowUseCase> =
        object : dagger.Lazy<CurrentSessionFlowUseCase> {
            override fun get(): CurrentSessionFlowUseCase = currentSessionFlow
        }

    @Provides
    fun provideDoesValidSessionExistUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DoesValidSessionExistUseCase =
        coreLogic.getGlobalScope().doesValidSessionExist

    @Provides
    fun provideDoesValidSessionExistUseCaseLazy(
        doesValidSessionExist: DoesValidSessionExistUseCase,
    ): dagger.Lazy<DoesValidSessionExistUseCase> =
        object : dagger.Lazy<DoesValidSessionExistUseCase> {
            override fun get(): DoesValidSessionExistUseCase = doesValidSessionExist
        }

    @Provides
    fun provideGetServerConfigUseCase(@KaliumCoreLogic coreLogic: CoreLogic): GetServerConfigUseCase =
        coreLogic.getGlobalScope().fetchServerConfigFromDeepLink

    @Provides
    fun provideGetServerConfigUseCaseLazy(getServerConfig: GetServerConfigUseCase): dagger.Lazy<GetServerConfigUseCase> =
        object : dagger.Lazy<GetServerConfigUseCase> {
            override fun get(): GetServerConfigUseCase = getServerConfig
        }

    @Provides
    fun provideObserveSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessionsFlow

    @Provides
    fun provideObserveSessionsUseCaseLazy(observeSessions: ObserveSessionsUseCase): dagger.Lazy<ObserveSessionsUseCase> =
        object : dagger.Lazy<ObserveSessionsUseCase> {
            override fun get(): ObserveSessionsUseCase = observeSessions
        }

    @Provides
    fun provideObserveIfAppUpdateRequiredUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ObserveIfAppUpdateRequiredUseCase =
        coreLogic.getGlobalScope().observeIfAppUpdateRequired

    @Provides
    fun provideObserveIfAppUpdateRequiredUseCaseLazy(
        observeIfAppUpdateRequired: ObserveIfAppUpdateRequiredUseCase,
    ): dagger.Lazy<ObserveIfAppUpdateRequiredUseCase> =
        object : dagger.Lazy<ObserveIfAppUpdateRequiredUseCase> {
            override fun get(): ObserveIfAppUpdateRequiredUseCase = observeIfAppUpdateRequired
        }

    @Provides
    fun provideObserveNewClientsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveNewClientsUseCase =
        coreLogic.getGlobalScope().observeNewClientsUseCase

    @Provides
    fun provideObserveNewClientsUseCaseLazy(observeNewClients: ObserveNewClientsUseCase): dagger.Lazy<ObserveNewClientsUseCase> =
        object : dagger.Lazy<ObserveNewClientsUseCase> {
            override fun get(): ObserveNewClientsUseCase = observeNewClients
        }

    @Provides
    fun provideClearNewClientsForUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ClearNewClientsForUserUseCase =
        coreLogic.getGlobalScope().clearNewClientsForUser

    @Provides
    fun provideClearNewClientsForUserUseCaseLazy(
        clearNewClientsForUser: ClearNewClientsForUserUseCase,
    ): dagger.Lazy<ClearNewClientsForUserUseCase> =
        object : dagger.Lazy<ClearNewClientsForUserUseCase> {
            override fun get(): ClearNewClientsForUserUseCase = clearNewClientsForUser
        }

    @Provides
    fun provideDoesValidNomadAccountExistUseCaseLazy(
        doesValidNomadAccountExist: DoesValidNomadAccountExistUseCase,
    ): dagger.Lazy<DoesValidNomadAccountExistUseCase> =
        object : dagger.Lazy<DoesValidNomadAccountExistUseCase> {
            override fun get(): DoesValidNomadAccountExistUseCase = doesValidNomadAccountExist
        }

    @Provides
    fun provideAccountSwitchUseCaseLazy(accountSwitch: AccountSwitchUseCase): dagger.Lazy<AccountSwitchUseCase> =
        object : dagger.Lazy<AccountSwitchUseCase> {
            override fun get(): AccountSwitchUseCase = accountSwitch
        }

    @Provides
    fun provideServicesManager(entryPoint: WireMetroHiltEntryPoint): ServicesManager =
        entryPoint.servicesManager()

    @Provides
    fun provideServicesManagerLazy(servicesManager: ServicesManager): dagger.Lazy<ServicesManager> =
        object : dagger.Lazy<ServicesManager> {
            override fun get(): ServicesManager = servicesManager
        }

    @Provides
    fun provideWorkManagerLazy(workManager: WorkManager): dagger.Lazy<WorkManager> =
        object : dagger.Lazy<WorkManager> {
            override fun get(): WorkManager = workManager
        }

    @Provides
    fun provideCurrentScreenManagerLazy(currentScreenManager: CurrentScreenManager): dagger.Lazy<CurrentScreenManager> =
        object : dagger.Lazy<CurrentScreenManager> {
            override fun get(): CurrentScreenManager = currentScreenManager
        }

    @Provides
    fun provideObserveSyncStateUseCaseProviderFactory(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ObserveSyncStateUseCaseProvider.Factory =
        object : ObserveSyncStateUseCaseProvider.Factory {
            override fun create(userId: UserId): ObserveSyncStateUseCaseProvider =
                ObserveSyncStateUseCaseProvider(coreLogic, userId)
        }

    @Provides
    fun provideObserveScreenshotCensoringConfigUseCaseProviderFactory(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ObserveScreenshotCensoringConfigUseCaseProvider.Factory =
        object : ObserveScreenshotCensoringConfigUseCaseProvider.Factory {
            override fun create(userId: UserId): ObserveScreenshotCensoringConfigUseCaseProvider =
                ObserveScreenshotCensoringConfigUseCaseProvider(coreLogic, userId)
        }

    @Provides
    fun provideObserveIfE2EIRequiredDuringLoginUseCaseProviderFactory(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ObserveIfE2EIRequiredDuringLoginUseCaseProvider.Factory =
        object : ObserveIfE2EIRequiredDuringLoginUseCaseProvider.Factory {
            override fun create(userId: UserId): ObserveIfE2EIRequiredDuringLoginUseCaseProvider =
                ObserveIfE2EIRequiredDuringLoginUseCaseProvider(coreLogic, userId)
        }

    @Provides
    fun provideIsProfileQRCodeEnabledUseCaseProviderFactory(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): IsProfileQRCodeEnabledUseCaseProvider.Factory =
        object : IsProfileQRCodeEnabledUseCaseProvider.Factory {
            override fun create(userId: UserId): IsProfileQRCodeEnabledUseCaseProvider =
                IsProfileQRCodeEnabledUseCaseProvider(coreLogic, userId)
        }

    @Provides
    fun provideObserveSelfUserUseCaseProviderFactory(
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): ObserveSelfUserUseCaseProvider.Factory =
        object : ObserveSelfUserUseCaseProvider.Factory {
            override fun create(userId: UserId): ObserveSelfUserUseCaseProvider =
                ObserveSelfUserUseCaseProvider(coreLogic, userId)
        }

    @Provides
    fun provideDeepLinkProcessor(
        accountSwitch: AccountSwitchUseCase,
        currentSession: CurrentSessionUseCase,
        @KaliumCoreLogic coreLogic: CoreLogic,
    ): DeepLinkProcessor =
        DeepLinkProcessor(accountSwitch, currentSession, coreLogic)

    @Provides
    fun provideDeepLinkProcessorLazy(deepLinkProcessor: DeepLinkProcessor): dagger.Lazy<DeepLinkProcessor> =
        object : dagger.Lazy<DeepLinkProcessor> {
            override fun get(): DeepLinkProcessor = deepLinkProcessor
        }

    @Provides
    fun provideIntentsProcessor(): IntentsProcessor =
        IntentsProcessor(NomadIntentSignatureValidator())

    @Provides
    fun provideIntentsProcessorLazy(intentsProcessor: IntentsProcessor): dagger.Lazy<IntentsProcessor> =
        object : dagger.Lazy<IntentsProcessor> {
            override fun get(): IntentsProcessor = intentsProcessor
        }

    @Provides
    fun provideWireActivityIntentGateway(
        deepLinkProcessor: dagger.Lazy<DeepLinkProcessor>,
        intentsProcessor: dagger.Lazy<IntentsProcessor>,
    ): WireActivityIntentGateway =
        AndroidWireActivityIntentGateway(deepLinkProcessor, intentsProcessor)

    @Provides
    fun provideWireActivityIntentGatewayLazy(
        intentGateway: WireActivityIntentGateway,
    ): dagger.Lazy<WireActivityIntentGateway> =
        object : dagger.Lazy<WireActivityIntentGateway> {
            override fun get(): WireActivityIntentGateway = intentGateway
        }

    @Provides
    fun provideMonitorSyncWorkUseCase(
        @ApplicationContext context: Context,
        @KaliumCoreLogic coreLogic: CoreLogic,
        observeSessions: ObserveSessionsUseCase,
    ): MonitorSyncWorkUseCase =
        MonitorSyncWorkUseCase(context, coreLogic, observeSessions)

    @Provides
    fun provideIsAnalyticsAvailableUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        analyticsConfiguration: AnalyticsConfiguration,
        userDataStoreProvider: UserDataStoreProvider,
    ): IsAnalyticsAvailableUseCase =
        IsAnalyticsAvailableUseCase(coreLogic, analyticsConfiguration, userDataStoreProvider)

    @Provides
    fun provideIsAnalyticsAvailableUseCaseLazy(
        isAnalyticsAvailableUseCase: IsAnalyticsAvailableUseCase,
    ): dagger.Lazy<IsAnalyticsAvailableUseCase> =
        object : dagger.Lazy<IsAnalyticsAvailableUseCase> {
            override fun get(): IsAnalyticsAvailableUseCase = isAnalyticsAvailableUseCase
        }

    @Provides
    fun provideSelfServerConfigUseCaseLazy(selfServerConfig: SelfServerConfigUseCase): dagger.Lazy<SelfServerConfigUseCase> =
        object : dagger.Lazy<SelfServerConfigUseCase> {
            override fun get(): SelfServerConfigUseCase = selfServerConfig
        }

    @Provides
    fun provideDisableAppLockUseCaseLazy(disableAppLockUseCase: DisableAppLockUseCase): dagger.Lazy<DisableAppLockUseCase> =
        object : dagger.Lazy<DisableAppLockUseCase> {
            override fun get(): DisableAppLockUseCase = disableAppLockUseCase
        }

    @Provides
    fun provideKaliumConfigs(): KaliumConfigs =
        KaliumConfigs(
            fileRestrictionState = lazy {
                if (BuildConfig.FILE_RESTRICTION_ENABLED) {
                    BuildConfig.FILE_RESTRICTION_LIST.split(",").map { it.trim() }.let {
                        BuildFileRestrictionState.AllowSome(it)
                    }
                } else {
                    BuildFileRestrictionState.NoRestriction
                }
            },
            forceConstantBitrateCalls = BuildConfig.FORCE_CONSTANT_BITRATE_CALLS,
            shouldEncryptData = { !BuildConfig.DEBUG || Build.VERSION.SDK_INT < Build.VERSION_CODES.R },
            lowerKeyPackageLimits = BuildConfig.LOWER_KEYPACKAGE_LIMIT,
            developmentApiEnabled = BuildConfig.DEVELOPMENT_API_ENABLED,
            ignoreSSLCertificatesForUnboundCalls = BuildConfig.IGNORE_SSL_CERTIFICATES,
            encryptProteusStorage = true,
            guestRoomLink = BuildConfig.ENABLE_GUEST_ROOM_LINK,
            selfDeletingMessages = BuildConfig.SELF_DELETING_MESSAGES,
            wipeOnCookieInvalid = BuildConfig.WIPE_ON_COOKIE_INVALID,
            wipeOnDeviceRemoval = BuildConfig.WIPE_ON_DEVICE_REMOVAL,
            wipeOnRootedDevice = BuildConfig.WIPE_ON_ROOTED_DEVICE,
            certPinningConfig = BuildConfig.CERTIFICATE_PINNING_CONFIG,
            maxRemoteSearchResultCount = BuildConfig.MAX_REMOTE_SEARCH_RESULT_COUNT,
            limitTeamMembersFetchDuringSlowSync = BuildConfig.LIMIT_TEAM_MEMBERS_FETCH_DURING_SLOW_SYNC,
            isMlsResetEnabled = BuildConfig.IS_MLS_RESET_ENABLED,
            collaboraIntegration = BuildConfig.COLLABORA_INTEGRATION_ENABLED,
            dbInvalidationControlEnabled = BuildConfig.DB_INVALIDATION_CONTROL_ENABLED,
            domainWithFaultyKeysMap = BuildConfig.DOMAIN_REMOVAL_KEYS_FOR_REPAIR,
            isDebug = BuildConfig.DEBUG,
        )

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
    fun provideDeleteAssetUseCase(userScope: UserScope): DeleteAssetUseCase =
        userScope.deleteAsset

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
    fun provideAudioNormalizedLoudnessBuilder(@KaliumCoreLogic coreLogic: CoreLogic): AudioNormalizedLoudnessBuilder =
        coreLogic.audioNormalizedLoudnessBuilder

    @ApplicationScope
    @Provides
    fun provideApplicationCoroutineScope(entryPoint: WireMetroHiltEntryPoint): CoroutineScope =
        entryPoint.applicationScope()

    @Provides
    fun provideMusicMediaPlayer(): MediaPlayer =
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }

    @Provides
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    fun provideRecordAudioMessagePlayer(
        @ApplicationContext context: Context,
        mediaPlayer: MediaPlayer,
        audioFocusHelper: AudioFocusHelper,
        @ApplicationScope applicationScope: CoroutineScope,
    ): RecordAudioMessagePlayer =
        RecordAudioMessagePlayer(
            context = context,
            audioMediaPlayer = mediaPlayer,
            audioFocusHelper = audioFocusHelper,
            scope = applicationScope,
        )

    @Provides
    fun provideConversationAudioMessagePlayer(entryPoint: WireMetroHiltEntryPoint): ConversationAudioMessagePlayer =
        entryPoint.conversationAudioMessagePlayer()

    @Provides
    fun provideScreenStateObserver(@ApplicationContext context: Context): ScreenStateObserver =
        ScreenStateObserver(context)

    @Provides
    fun provideCurrentScreenManager(entryPoint: WireMetroHiltEntryPoint): CurrentScreenManager =
        entryPoint.currentScreenManager()

    @Provides
    fun provideSwitchAccountObserver(entryPoint: WireMetroHiltEntryPoint): SwitchAccountObserver =
        entryPoint.switchAccountObserver()

    @Provides
    fun provideNetworkUtil(@ApplicationContext context: Context): NetworkUtil =
        NetworkUtil(context)

    @Provides
    fun provideManagedConfigurationsReporter(
        @ApplicationContext context: Context,
    ): ManagedConfigurationsReporter =
        ManagedConfigurationsReporter(context)

    @Provides
    fun provideManagedConfigurationsReceiver(
        managedConfigurationsManager: ManagedConfigurationsManager,
        managedConfigurationsReporter: ManagedConfigurationsReporter,
        @KaliumCoreLogic coreLogic: dagger.Lazy<CoreLogic>,
        startPersistentWebsocketIfNecessary: StartPersistentWebsocketIfNecessaryUseCase,
        dispatcherProvider: DispatcherProvider,
    ): ManagedConfigurationsReceiver =
        ManagedConfigurationsReceiver(
            managedConfigurationsManager = managedConfigurationsManager,
            managedConfigurationsReporter = managedConfigurationsReporter,
            coreLogic = coreLogic,
            startPersistentWebsocketIfNecessary = startPersistentWebsocketIfNecessary,
            dispatcher = dispatcherProvider,
        )

    @Provides
    fun provideDynamicReceiversManager(
        @ApplicationContext context: Context,
        managedConfigurationsReceiver: ManagedConfigurationsReceiver,
    ): DynamicReceiversManager =
        DynamicReceiversManager(
            context = context,
            managedConfigurationsReceiver = managedConfigurationsReceiver,
        )

    @Provides
    fun provideProximitySensorManager(entryPoint: WireMetroHiltEntryPoint): ProximitySensorManager =
        entryPoint.proximitySensorManager()

    @Provides
    fun provideRecordAudioFileGateway(
        @ApplicationContext context: Context,
        generateAudioFileWithEffects: GenerateAudioFileWithEffectsUseCase,
    ): RecordAudioFileGateway =
        AndroidRecordAudioFileGateway(
            context = context,
            generateAudioFileWithEffects = generateAudioFileWithEffects,
        )

    @Provides
    fun provideAudioMediaRecorder(
        kaliumFileSystem: KaliumFileSystem,
        dispatchers: DispatcherProvider,
    ): AudioMediaRecorder =
        AudioMediaRecorder(
            kaliumFileSystem = kaliumFileSystem,
            dispatcherProvider = dispatchers,
        )

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
    fun provideFetchConversationMLSVerificationStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): FetchConversationMLSVerificationStatusUseCase =
        coreLogic.getSessionScope(currentAccount).fetchConversationMLSVerificationStatus

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
    fun provideMigrateFromPersonalToTeamUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): MigrateFromPersonalToTeamUseCase =
        coreLogic.getSessionScope(currentAccount).migrateFromPersonalToTeam

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
    fun provideUICallParticipantMapper(userTypeMapper: UserTypeMapper): UICallParticipantMapper =
        UICallParticipantMapper(userTypeMapper)

    @Provides
    fun provideContactMapper(userTypeMapper: UserTypeMapper): ContactMapper =
        ContactMapper(userTypeMapper)

    @Provides
    fun provideMessageResourceProvider(): MessageResourceProvider =
        MessageResourceProvider()

    @Provides
    fun provideGetMediaMetadataUseCase(): GetMediaMetadataUseCase =
        GetMediaMetadataUseCaseImpl()

    @Provides
    fun provideImageUtil(): ImageUtil =
        ImageUtil

    @Provides
    fun providePingRinger(@ApplicationContext context: Context): PingRinger =
        PingRinger(context)

    @Provides
    fun provideTempWritableAttachmentUriProvider(
        fileManager: FileManager,
        kaliumFileSystem: KaliumFileSystem,
    ): TempWritableAttachmentUriProvider =
        AndroidTempWritableAttachmentUriProvider(
            fileManager = fileManager,
            kaliumFileSystem = kaliumFileSystem,
        )

    @Provides
    fun provideMessageAttachmentAssetImporter(
        handleUriAsset: HandleUriAssetUseCase,
    ): MessageAttachmentAssetImporter =
        MessageAttachmentAssetImporterImpl(handleUriAsset)

    @Provides
    fun provideMessageAttachmentFileGateway(
        fileManager: FileManager,
    ): MessageAttachmentFileGateway =
        MessageAttachmentFileGatewayImpl(fileManager)

    @Provides
    fun provideISOFormatter(): ISOFormatter =
        ISOFormatter()

    @Provides
    fun provideUIAssetMapper(): UIAssetMapper =
        UIAssetMapper()

    @Provides
    fun provideRegularMessageMapper(
        messageResourceProvider: MessageResourceProvider,
        isoFormatter: ISOFormatter,
    ): RegularMessageMapper =
        RegularMessageMapper(messageResourceProvider, isoFormatter)

    @Provides
    fun provideSystemMessageContentMapper(messageResourceProvider: MessageResourceProvider): SystemMessageContentMapper =
        SystemMessageContentMapper(messageResourceProvider)

    @Provides
    fun provideMessageContentMapper(
        regularMessageMapper: RegularMessageMapper,
        systemMessageContentMapper: SystemMessageContentMapper,
    ): MessageContentMapper =
        MessageContentMapper(regularMessageMapper, systemMessageContentMapper)

    @Provides
    fun provideMessageMapper(
        userTypeMapper: UserTypeMapper,
        messageContentMapper: MessageContentMapper,
        isoFormatter: ISOFormatter,
    ): MessageMapper =
        MessageMapper(userTypeMapper, messageContentMapper, isoFormatter)

    @Provides
    fun provideUIParticipantMapper(userTypeMapper: UserTypeMapper): UIParticipantMapper =
        UIParticipantMapper(userTypeMapper)

    @Provides
    fun provideGetSelfUserUseCase(userScope: UserScope): GetSelfUserUseCase =
        userScope.getSelfUser

    @Provides
    fun provideGetMembersE2EICertificateStatusesUseCase(userScope: UserScope): GetMembersE2EICertificateStatusesUseCase =
        userScope.getMembersE2EICertificateStatuses

    @Provides
    fun provideRefreshUsersWithoutMetadataUseCase(userScope: UserScope): RefreshUsersWithoutMetadataUseCase =
        userScope.refreshUsersWithoutMetadata

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
    fun provideIsFileSharingEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): IsFileSharingEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isFileSharingEnabled

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
    fun provideNeedsToRegisterClientUseCase(clientScope: ClientScope): NeedsToRegisterClientUseCase =
        clientScope.needsToRegisterClient

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
    fun provideIsMLSEnabledUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): IsMLSEnabledUseCase =
        coreLogic.getSessionScope(currentAccount).isMLSEnabled

    @Provides
    fun provideIsWireCellsEnabledUseCase(userScope: UserScope): IsWireCellsEnabledUseCase =
        userScope.isWireCellsEnabled

    @Provides
    fun provideIsWireCellsEnabledForConversationUseCase(userScope: UserScope): IsWireCellsEnabledForConversationUseCase =
        userScope.isWireCellsEnabledForConversation

    @Provides
    fun provideForegroundActionsUseCase(userScope: UserScope): ForegroundActionsUseCase =
        userScope.foregroundActions

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
    fun provideObserveConversationListDetailsWithEventsUseCase(
        conversationScope: ConversationScope,
    ): ObserveConversationListDetailsWithEventsUseCase =
        conversationScope.observeConversationListDetailsWithEvents

    @Provides
    fun provideGetConversationUnreadEventsCountUseCase(conversationScope: ConversationScope): GetConversationUnreadEventsCountUseCase =
        conversationScope.getConversationUnreadEventsCountUseCase

    @Provides
    fun provideClearUsersTypingEventsUseCase(conversationScope: ConversationScope): ClearUsersTypingEventsUseCase =
        conversationScope.clearUsersTypingEvents

    @Provides
    fun provideRefreshConversationsWithoutMetadataUseCase(
        conversationScope: ConversationScope,
    ): RefreshConversationsWithoutMetadataUseCase =
        conversationScope.refreshConversationsWithoutMetadata

    @Provides
    fun provideObserveArchivedUnreadConversationsCountUseCase(
        conversationScope: ConversationScope,
    ): ObserveArchivedUnreadConversationsCountUseCase =
        conversationScope.observeArchivedUnreadConversationsCount

    @Provides
    fun provideObserveArchivedUnreadConversationsCountUseCaseLazy(
        observeArchivedUnreadConversationsCount: ObserveArchivedUnreadConversationsCountUseCase,
    ): dagger.Lazy<ObserveArchivedUnreadConversationsCountUseCase> =
        object : dagger.Lazy<ObserveArchivedUnreadConversationsCountUseCase> {
            override fun get(): ObserveArchivedUnreadConversationsCountUseCase = observeArchivedUnreadConversationsCount
        }

    @Provides
    fun provideNotifyConversationIsOpenUseCase(conversationScope: ConversationScope): NotifyConversationIsOpenUseCase =
        conversationScope.notifyConversationIsOpen

    @Provides
    fun provideObserveConversationInteractionAvailabilityUseCase(
        conversationScope: ConversationScope,
    ): ObserveConversationInteractionAvailabilityUseCase =
        conversationScope.observeConversationInteractionAvailabilityUseCase

    @Provides
    fun provideMembersToMentionUseCase(conversationScope: ConversationScope): MembersToMentionUseCase =
        conversationScope.getMembersToMention

    @Provides
    fun provideUpdateConversationReadDateUseCase(conversationScope: ConversationScope): UpdateConversationReadDateUseCase =
        conversationScope.updateConversationReadDateUseCase

    @Provides
    fun provideMarkConversationAsReadLocallyUseCase(conversationScope: ConversationScope): MarkConversationAsReadLocallyUseCase =
        conversationScope.markConversationAsReadLocally

    @Provides
    fun provideSendTypingEventUseCase(conversationScope: ConversationScope): SendTypingEventUseCase =
        conversationScope.sendTypingEvent

    @Provides
    fun provideSetUserInformedAboutVerificationUseCase(
        conversationScope: ConversationScope,
    ): SetUserInformedAboutVerificationUseCase =
        conversationScope.setUserInformedAboutVerificationBeforeMessagingUseCase

    @Provides
    fun provideObserveDegradedConversationNotifiedUseCase(
        conversationScope: ConversationScope,
    ): ObserveDegradedConversationNotifiedUseCase =
        conversationScope.observeInformAboutVerificationBeforeMessagingFlagUseCase

    @Provides
    fun provideSetNotifiedAboutConversationUnderLegalHoldUseCase(
        conversationScope: ConversationScope,
    ): SetNotifiedAboutConversationUnderLegalHoldUseCase =
        conversationScope.setNotifiedAboutConversationUnderLegalHold

    @Provides
    fun provideObserveConversationUnderLegalHoldNotifiedUseCase(
        conversationScope: ConversationScope,
    ): ObserveConversationUnderLegalHoldNotifiedUseCase =
        conversationScope.observeConversationUnderLegalHoldNotified

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
    fun provideMoveConversationToFolderUseCase(conversationScope: ConversationScope): MoveConversationToFolderUseCase =
        conversationScope.moveConversationToFolder

    @Provides
    fun provideObserveConversationMembersUseCase(conversationScope: ConversationScope): ObserveConversationMembersUseCase =
        conversationScope.observeConversationMembers

    @Provides
    fun provideCreateRegularGroupUseCase(conversationScope: ConversationScope): CreateRegularGroupUseCase =
        conversationScope.createRegularGroup

    @Provides
    fun provideCreateChannelUseCase(conversationScope: ConversationScope): CreateChannelUseCase =
        conversationScope.createChannel

    @Provides
    fun provideObserveUsersTypingUseCase(conversationScope: ConversationScope): ObserveUsersTypingUseCase =
        conversationScope.observeUsersTyping

    @Provides
    fun provideObserveUserListByIdUseCase(conversationScope: ConversationScope): ObserveUserListByIdUseCase =
        conversationScope.observeUserListById

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
    fun provideUpdateConversationReceiptModeUseCase(conversationScope: ConversationScope): UpdateConversationReceiptModeUseCase =
        conversationScope.updateConversationReceiptMode

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
    fun provideObserveParticipantsForConversationUseCase(
        observeConversationMembers: ObserveConversationMembersUseCase,
        getMembersE2EICertificateStatuses: GetMembersE2EICertificateStatusesUseCase,
        uiParticipantMapper: UIParticipantMapper,
        dispatchers: DispatcherProvider,
    ): ObserveParticipantsForConversationUseCase =
        ObserveParticipantsForConversationUseCase(
            observeConversationMembers = observeConversationMembers,
            getMembersE2EICertificateStatuses = getMembersE2EICertificateStatuses,
            uiParticipantMapper = uiParticipantMapper,
            dispatchers = dispatchers,
        )

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
    fun provideRenameConversationUseCase(conversationScope: ConversationScope): RenameConversationUseCase =
        conversationScope.renameConversation

    @Provides
    fun provideUpdateMessageTimerUseCase(conversationScope: ConversationScope): UpdateMessageTimerUseCase =
        conversationScope.updateMessageTimer

    @Provides
    fun provideUpdateConversationAccessRoleUseCase(conversationScope: ConversationScope): UpdateConversationAccessRoleUseCase =
        conversationScope.updateConversationAccess

    @Provides
    fun provideChangeAccessForAppsInConversationUseCase(conversationScope: ConversationScope): ChangeAccessForAppsInConversationUseCase =
        conversationScope.changeAccessForAppsInConversation

    @Provides
    fun provideCanCreatePasswordProtectedLinksUseCase(conversationScope: ConversationScope): CanCreatePasswordProtectedLinksUseCase =
        conversationScope.canCreatePasswordProtectedLinks

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
    fun provideSyncConversationCodeUseCase(conversationScope: ConversationScope): SyncConversationCodeUseCase =
        conversationScope.syncConversationCode

    @Provides
    fun provideObserveGuestRoomLinkFeatureFlagUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveGuestRoomLinkFeatureFlagUseCase =
        coreLogic.getSessionScope(currentAccount).observeGuestRoomLinkFeatureFlag

    @Provides
    fun provideRandomPassword(): RandomPassword =
        RandomPassword()

    @Provides
    fun provideChannelsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ChannelsScope =
        coreLogic.getSessionScope(currentAccount).channels

    @Provides
    fun provideUpdateChannelAddPermissionUseCase(channelsScope: ChannelsScope): UpdateChannelAddPermissionUseCase =
        channelsScope.updateChannelAddPermission

    @Provides
    fun provideObserveChannelsCreationPermissionUseCase(
        channelsScope: ChannelsScope,
    ): ObserveChannelsCreationPermissionUseCase =
        channelsScope.observeChannelsCreationPermissionUseCase

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
    fun provideSearchScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): SearchScope =
        coreLogic.getSessionScope(currentAccount).search

    @Provides
    fun provideSearchUsersUseCase(searchScope: SearchScope): SearchUsersUseCase =
        searchScope.searchUsers

    @Provides
    fun provideSearchByHandleUseCase(searchScope: SearchScope): SearchByHandleUseCase =
        searchScope.searchByHandle

    @Provides
    fun provideFederatedSearchParser(searchScope: SearchScope): FederatedSearchParser =
        searchScope.federatedSearchParser

    @Provides
    fun provideIsFederationSearchAllowedUseCase(searchScope: SearchScope): IsFederationSearchAllowedUseCase =
        searchScope.isFederationSearchAllowedUseCase

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
    fun provideSearchAppsByNameUseCase(appScope: AppScope): SearchAppsByNameUseCase =
        appScope.searchAppsByName

    @Provides
    fun provideObserveAllAppsUseCase(appScope: AppScope): ObserveAllAppsUseCase =
        appScope.observeAllApps

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
    fun provideObserveAllServicesUseCase(serviceScope: ServiceScope): ObserveAllServicesUseCase =
        serviceScope.observeAllServices

    @Provides
    fun provideSearchServicesByNameUseCase(serviceScope: ServiceScope): SearchServicesByNameUseCase =
        serviceScope.searchServicesByName

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
    fun provideGetMessageByIdUseCase(messageScope: MessageScope): GetMessageByIdUseCase =
        messageScope.getMessageById

    @Provides
    fun provideUpdateAssetMessageTransferStatusUseCase(messageScope: MessageScope): UpdateAssetMessageTransferStatusUseCase =
        messageScope.updateAssetMessageTransferStatus

    @Provides
    fun provideObserveAssetStatusesUseCase(messageScope: MessageScope): ObserveAssetStatusesUseCase =
        messageScope.observeAssetStatuses

    @Provides
    fun provideFetchOlderNomadMessagesByConversationUseCase(messageScope: MessageScope): FetchOlderNomadMessagesByConversationUseCase =
        messageScope.fetchOlderMessagesByConversationId

    @Provides
    fun provideToggleReactionUseCase(messageScope: MessageScope): ToggleReactionUseCase =
        messageScope.toggleReaction

    @Provides
    fun provideResetSessionUseCase(messageScope: MessageScope): ResetSessionUseCase =
        messageScope.resetSession

    @Provides
    fun provideGetSearchedConversationMessagePositionUseCase(messageScope: MessageScope): GetSearchedConversationMessagePositionUseCase =
        messageScope.getSearchedConversationMessagePosition

    @Provides
    fun provideSendButtonActionMessageUseCase(messageScope: MessageScope): SendButtonActionMessageUseCase =
        messageScope.sendButtonActionMessage

    @Provides
    fun provideEnqueueMessageSelfDeletionUseCase(messageScope: MessageScope): EnqueueMessageSelfDeletionUseCase =
        messageScope.enqueueMessageSelfDeletion

    @Provides
    fun provideScheduleNewAssetMessageUseCase(messageScope: MessageScope): ScheduleNewAssetMessageUseCase =
        messageScope.sendAssetMessage

    @Provides
    fun provideSendTextMessageUseCase(messageScope: MessageScope): SendTextMessageUseCase =
        messageScope.sendTextMessage

    @Provides
    fun provideSendMultipartMessageUseCase(messageScope: MessageScope): SendMultipartMessageUseCase =
        messageScope.sendMultipartMessage

    @Provides
    fun provideSendEditTextMessageUseCase(messageScope: MessageScope): SendEditTextMessageUseCase =
        messageScope.sendEditTextMessage

    @Provides
    fun provideSendEditMultipartMessageUseCase(messageScope: MessageScope): SendEditMultipartMessageUseCase =
        messageScope.sendEditMultipartMessage

    @Provides
    fun provideRetryFailedMessageUseCase(messageScope: MessageScope): RetryFailedMessageUseCase =
        messageScope.retryFailedMessage

    @Provides
    fun provideSendKnockUseCase(messageScope: MessageScope): SendKnockUseCase =
        messageScope.sendKnock

    @Provides
    fun provideSendLocationUseCase(messageScope: MessageScope): SendLocationUseCase =
        messageScope.sendLocation

    @Provides
    fun provideRemoveMessageDraftUseCase(messageScope: MessageScope): RemoveMessageDraftUseCase =
        messageScope.removeMessageDraftUseCase

    @Provides
    fun provideGetMessageDraftUseCase(messageScope: MessageScope): GetMessageDraftUseCase =
        messageScope.getMessageDraftUseCase

    @Provides
    fun provideSaveMessageDraftUseCase(messageScope: MessageScope): SaveMessageDraftUseCase =
        messageScope.saveMessageDraftUseCase

    @Provides
    fun provideGetPaginatedFlowOfMessagesByConversationUseCase(
        messageScope: MessageScope,
    ): GetPaginatedFlowOfMessagesByConversationUseCase =
        messageScope.getPaginatedFlowOfMessagesByConversation

    @Provides
    fun provideGetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase(
        messageScope: MessageScope,
    ): GetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase =
        messageScope.getPaginatedFlowOfMessagesBySearchQueryAndConversation

    @Provides
    fun provideGetPaginatedFlowOfAssetMessageByConversationIdUseCase(
        messageScope: MessageScope,
    ): GetPaginatedFlowOfAssetMessageByConversationIdUseCase =
        messageScope.getPaginatedFlowOfAssetMessageByConversationId

    @Provides
    fun provideObservePaginatedAssetImageMessages(messageScope: MessageScope): ObservePaginatedAssetImageMessages =
        messageScope.observePaginatedImageAssetMessageByConversationId

    @Provides
    fun provideObserveMessageReactionsUseCase(messageScope: MessageScope): ObserveMessageReactionsUseCase =
        messageScope.observeMessageReactions

    @Provides
    fun provideObserveMessageReceiptsUseCase(messageScope: MessageScope): ObserveMessageReceiptsUseCase =
        messageScope.observeMessageReceipts

    @Provides
    fun provideObserveMessageByIdUseCase(messageScope: MessageScope): ObserveMessageByIdUseCase =
        messageScope.observeMessageById

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
    fun provideGetPaginatedFilesFlowUseCase(cellsScope: CellsScope): GetPaginatedFilesFlowUseCase =
        cellsScope.paginatedFilesFlowUseCase

    @Provides
    fun provideGetPaginatedCellConversationsFlowUseCase(cellsScope: CellsScope): GetPaginatedCellConversationsFlowUseCase =
        cellsScope.paginatedConversationsFlowUseCase

    @Provides
    fun provideDeleteCellAssetUseCase(cellsScope: CellsScope): DeleteCellAssetUseCase =
        cellsScope.deleteCellAssetUseCase

    @Provides
    fun provideCreateFolderUseCase(cellsScope: CellsScope): CreateFolderUseCase =
        cellsScope.createFolderUseCase

    @Provides
    fun provideCreatePresentationFileUseCase(cellsScope: CellsScope): CreatePresentationFileUseCase =
        cellsScope.createPresentationFileUseCase

    @Provides
    fun provideCreateDocumentFileUseCase(cellsScope: CellsScope): CreateDocumentFileUseCase =
        cellsScope.createDocumentFileUseCase

    @Provides
    fun provideCreateSpreadsheetFileUseCase(cellsScope: CellsScope): CreateSpreadsheetFileUseCase =
        cellsScope.createSpreadsheetFileUseCase

    @Provides
    fun provideMoveNodeUseCase(cellsScope: CellsScope): MoveNodeUseCase =
        cellsScope.moveNodeUseCase

    @Provides
    fun provideGetFoldersUseCase(cellsScope: CellsScope): GetFoldersUseCase =
        cellsScope.getFoldersUseCase

    @Provides
    fun provideGetAllTagsUseCase(cellsScope: CellsScope): GetAllTagsUseCase =
        cellsScope.getAllTags

    @Provides
    fun provideUpdateNodeTagsUseCase(cellsScope: CellsScope): UpdateNodeTagsUseCase =
        cellsScope.updateNodeTagsUseCase

    @Provides
    fun provideRemoveNodeTagsUseCase(cellsScope: CellsScope): RemoveNodeTagsUseCase =
        cellsScope.removeNodeTagsUseCase

    @Provides
    fun provideRenameNodeUseCase(cellsScope: CellsScope): RenameNodeUseCase =
        cellsScope.renameNodeUseCase

    @Provides
    fun provideGetOwnersUseCase(cellsScope: CellsScope): GetOwnersUseCase =
        cellsScope.getOwnersUseCase

    @Provides
    fun provideCreatePublicLinkUseCase(cellsScope: CellsScope): CreatePublicLinkUseCase =
        cellsScope.createPublicLinkUseCase

    @Provides
    fun provideGetPublicLinkUseCase(cellsScope: CellsScope): GetPublicLinkUseCase =
        cellsScope.getPublicLinkUseCase

    @Provides
    fun provideDeletePublicLinkUseCase(cellsScope: CellsScope): DeletePublicLinkUseCase =
        cellsScope.deletePublicLinkUseCase

    @Provides
    fun provideCreatePublicLinkPasswordUseCase(cellsScope: CellsScope): CreatePublicLinkPasswordUseCase =
        cellsScope.createPublicLinkPasswordUseCase

    @Provides
    fun provideUpdatePublicLinkPasswordUseCase(cellsScope: CellsScope): UpdatePublicLinkPasswordUseCase =
        cellsScope.updatePublicLinkPasswordUseCase

    @Provides
    fun provideGetPublicLinkPasswordUseCase(cellsScope: CellsScope): GetPublicLinkPasswordUseCase =
        cellsScope.getPublicLinkPassword

    @Provides
    fun provideSetPublicLinkExpirationUseCase(cellsScope: CellsScope): SetPublicLinkExpirationUseCase =
        cellsScope.setPublicLinkExpiration

    @Provides
    fun provideGetNodeVersionsUseCase(cellsScope: CellsScope): GetNodeVersionsUseCase =
        cellsScope.getNodeVersions

    @Provides
    fun provideRestoreNodeVersionUseCase(cellsScope: CellsScope): RestoreNodeVersionUseCase =
        cellsScope.restoreNodeVersion

    @Provides
    fun provideDownloadCellVersionUseCase(cellsScope: CellsScope): DownloadCellVersionUseCase =
        cellsScope.downloadCellVersion

    @Provides
    fun provideRestoreNodeFromRecycleBinUseCase(cellsScope: CellsScope): RestoreNodeFromRecycleBinUseCase =
        cellsScope.restoreNodeFromRecycleBin

    @Provides
    fun provideIsAtLeastOneCellAvailableUseCase(cellsScope: CellsScope): IsAtLeastOneCellAvailableUseCase =
        cellsScope.isCellAvailable

    @Provides
    fun provideObserveAttachmentDraftsUseCase(cellsScope: CellsScope): ObserveAttachmentDraftsUseCase =
        cellsScope.observeAttachments

    @Provides
    fun provideAddAttachmentDraftUseCase(cellsScope: CellsScope): AddAttachmentDraftUseCase =
        cellsScope.addAttachment

    @Provides
    fun provideRemoveAttachmentDraftUseCase(cellsScope: CellsScope): RemoveAttachmentDraftUseCase =
        cellsScope.removeAttachment

    @Provides
    fun provideRetryAttachmentUploadUseCase(cellsScope: CellsScope): RetryAttachmentUploadUseCase =
        cellsScope.retryAttachmentUpload

    @Provides
    fun provideCellUploadManager(cellsScope: CellsScope): CellUploadManager =
        cellsScope.uploadManager

    @Provides
    fun provideGetCellFileUseCase(cellsScope: CellsScope): GetCellFileUseCase =
        cellsScope.getCellFileUseCase

    @Provides
    fun provideDownloadCellFileUseCase(cellsScope: CellsScope): DownloadCellFileUseCase =
        cellsScope.downloadCellFile

    @Provides
    fun provideRefreshCellAssetStateUseCase(cellsScope: CellsScope): RefreshCellAssetStateUseCase =
        cellsScope.refreshAsset

    @Provides
    fun provideGetEditorUrlUseCase(cellsScope: CellsScope): GetEditorUrlUseCase =
        cellsScope.getEditorUrl

    @Provides
    fun provideGetWireCellConfigurationUseCase(cellsScope: CellsScope): GetWireCellConfigurationUseCase =
        cellsScope.getCellConfig

    @Provides
    fun provideFileSizeFormatter(@ApplicationContext context: Context): FileSizeFormatter =
        FileSizeFormatter(context)

    @Provides
    fun provideCellFileExternalActions(androidCellFileExternalActions: AndroidCellFileExternalActions): CellFileExternalActions =
        androidCellFileExternalActions

    @Provides
    fun provideCellAssetRefreshHelper(
        refreshAsset: RefreshCellAssetStateUseCase,
        kaliumConfigs: KaliumConfigs,
    ): CellAssetRefreshHelper =
        CellAssetRefreshHelper(
            refreshAsset = refreshAsset,
            featureFlags = kaliumConfigs,
        )

    @Provides
    fun provideFileManager(@ApplicationContext context: Context): FileManager =
        FileManager(context)

    @Provides
    fun provideTimeZoneProvider(): TimeZoneProvider =
        TimeZoneProvider()

    @Provides
    fun provideConversationAssetFileGateway(fileManager: FileManager): ConversationAssetFileGateway =
        AndroidConversationAssetFileGateway(fileManager)

    @Provides
    fun provideUiTextResolver(@ApplicationContext context: Context): UiTextResolver =
        AndroidUiTextResolver(context)

    @Provides
    fun provideGetAssetSizeLimitUseCase(userScope: UserScope): GetAssetSizeLimitUseCase =
        userScope.getAssetSizeLimit

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
        gateway: AndroidExportObfuscatedCopyFileGateway,
    ): ExportObfuscatedCopyFileGateway = gateway

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
    fun provideObserveLastActiveCallWithSortedParticipantsUseCase(
        callsScope: CallsScope,
    ): ObserveLastActiveCallWithSortedParticipantsUseCase =
        callsScope.observeLastActiveCallWithSortedParticipants

    @Provides
    fun provideObserveOngoingCallsUseCase(callsScope: CallsScope): ObserveOngoingCallsUseCase =
        callsScope.observeOngoingCalls

    @Provides
    fun provideObserveOutgoingCallUseCase(callsScope: CallsScope): ObserveOutgoingCallUseCase =
        callsScope.observeOutgoingCall

    @Provides
    fun provideGetIncomingCallsUseCase(callsScope: CallsScope): GetIncomingCallsUseCase =
        callsScope.getIncomingCalls

    @Provides
    fun provideRejectCallUseCase(callsScope: CallsScope): RejectCallUseCase =
        callsScope.rejectCall

    @Provides
    fun provideAnswerCallUseCase(callsScope: CallsScope): AnswerCallUseCase =
        callsScope.answerCall

    @Provides
    fun provideEndCallUseCase(callsScope: CallsScope): EndCallUseCase =
        callsScope.endCall

    @Provides
    fun provideStartCallUseCase(callsScope: CallsScope): StartCallUseCase =
        callsScope.startCall

    @Provides
    fun provideIsLastCallClosedUseCase(callsScope: CallsScope): IsLastCallClosedUseCase =
        callsScope.isLastCallClosed

    @Provides
    fun provideMuteCallUseCase(callsScope: CallsScope): MuteCallUseCase =
        callsScope.muteCall

    @Provides
    fun provideUnMuteCallUseCase(callsScope: CallsScope): UnMuteCallUseCase =
        callsScope.unMuteCall

    @Provides
    fun provideSetVideoPreviewUseCase(callsScope: CallsScope): SetVideoPreviewUseCase =
        callsScope.setVideoPreview

    @Provides
    fun provideSetUIRotationUseCase(callsScope: CallsScope): SetUIRotationUseCase =
        callsScope.setUIRotation

    @Provides
    fun provideFlipToFrontCameraUseCase(callsScope: CallsScope): FlipToFrontCameraUseCase =
        callsScope.flipToFrontCamera

    @Provides
    fun provideFlipToBackCameraUseCase(callsScope: CallsScope): FlipToBackCameraUseCase =
        callsScope.flipToBackCamera

    @Provides
    fun provideTurnLoudSpeakerOffUseCase(callsScope: CallsScope): TurnLoudSpeakerOffUseCase =
        callsScope.turnLoudSpeakerOff

    @Provides
    fun provideTurnLoudSpeakerOnUseCase(callsScope: CallsScope): TurnLoudSpeakerOnUseCase =
        callsScope.turnLoudSpeakerOn

    @Provides
    fun provideObserveSpeakerUseCase(callsScope: CallsScope): ObserveSpeakerUseCase =
        callsScope.observeSpeaker

    @Provides
    fun provideUpdateVideoStateUseCase(callsScope: CallsScope): UpdateVideoStateUseCase =
        callsScope.updateVideoState

    @Provides
    fun provideRequestVideoStreamsUseCase(callsScope: CallsScope): RequestVideoStreamsUseCase =
        callsScope.requestVideoStreams

    @Provides
    fun provideSetVideoSendStateUseCase(callsScope: CallsScope): SetVideoSendStateUseCase =
        callsScope.setVideoSendState

    @Provides
    fun provideIsEligibleToStartCallUseCase(callsScope: CallsScope): IsEligibleToStartCallUseCase =
        callsScope.isEligibleToStartCall

    @Provides
    fun provideObserveConferenceCallingEnabledUseCase(callsScope: CallsScope): ObserveConferenceCallingEnabledUseCase =
        callsScope.observeConferenceCallingEnabled

    @Provides
    fun provideObserveInCallReactionsUseCase(callsScope: CallsScope): ObserveInCallReactionsUseCase =
        callsScope.observeInCallReactions

    @Provides
    fun provideObserveCallQualityDataUseCase(callsScope: CallsScope): ObserveCallQualityDataUseCase =
        callsScope.observeCallQualityData

    @Provides
    fun provideSetCallQualityIntervalUseCase(callsScope: CallsScope): SetCallQualityIntervalUseCase =
        callsScope.setCallQualityInterval

    @Provides
    fun provideObserveCallModerationActionsUseCase(callsScope: CallsScope): ObserveCallModerationActionsUseCase =
        callsScope.observeCallModerationActions

    @Provides
    fun provideSendInCallReactionUseCase(messageScope: MessageScope): SendInCallReactionUseCase =
        messageScope.sendInCallReactionUseCase

    @Provides
    fun provideNetworkStateObserver(@KaliumCoreLogic coreLogic: CoreLogic): NetworkStateObserver =
        coreLogic.networkStateObserver

    @Provides
    fun provideWireSessionImageLoader(
        @ApplicationContext context: Context,
        getAvatarAsset: GetAvatarAssetUseCase,
        deleteAsset: DeleteAssetUseCase,
        getMessageAsset: GetMessageAssetUseCase,
        networkStateObserver: NetworkStateObserver,
    ): WireSessionImageLoader =
        WireSessionImageLoader.Factory(
            context = context,
            getAvatarAsset = getAvatarAsset,
            deleteAsset = deleteAsset,
            networkStateObserver = networkStateObserver,
            getPrivateAsset = getMessageAsset,
        ).newImageLoader()

    @Provides
    fun provideCallRinger(@ApplicationContext context: Context): CallRinger =
        CallRinger(context)

    @Provides
    @Suppress("LongParameterList")
    fun provideHangUpCallUseCase(
        @ApplicationScope coroutineScope: CoroutineScope,
        observeEstablishedCalls: ObserveEstablishedCallsUseCase,
        observeSpeaker: ObserveSpeakerUseCase,
        endCall: EndCallUseCase,
        muteCall: MuteCallUseCase,
        turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase,
        flipToFrontCamera: FlipToFrontCameraUseCase,
        callRinger: CallRinger,
    ): HangUpCallUseCase =
        HangUpCallUseCase(
            coroutineScope = coroutineScope,
            observeEstablishedCalls = observeEstablishedCalls,
            observeSpeaker = observeSpeaker,
            endCall = endCall,
            muteCall = muteCall,
            turnLoudSpeakerOff = turnLoudSpeakerOff,
            flipToFrontCamera = flipToFrontCamera,
            callRinger = callRinger,
        )

    @Provides
    fun provideCallNotificationManager(entryPoint: WireMetroHiltEntryPoint): CallNotificationManager =
        entryPoint.callNotificationManager()

    @Provides
    fun provideCallServiceManager(@KaliumCoreLogic coreLogic: CoreLogic): CallServiceManager =
        CallServiceManager(coreLogic)

    @Provides
    fun providePersistentWebSocketServiceDependencies(
        @KaliumCoreLogic coreLogic: CoreLogic,
        dispatcherProvider: DispatcherProvider,
        notificationManager: WireNotificationManager,
        notificationChannelsManager: NotificationChannelsManager,
    ): PersistentWebSocketService.Dependencies =
        PersistentWebSocketService.Dependencies(
            coreLogic = coreLogic,
            dispatcherProvider = dispatcherProvider,
            notificationManager = notificationManager,
            notificationChannelsManager = notificationChannelsManager,
        )

    @Provides
    fun provideCallServiceDependencies(
        lifecycleManager: CallServiceManager,
        callNotificationManager: CallNotificationManager,
        dispatcherProvider: DispatcherProvider,
    ): CallService.Dependencies =
        CallService.Dependencies(
            lifecycleManager = lifecycleManager,
            callNotificationManager = callNotificationManager,
            dispatcherProvider = dispatcherProvider,
        )

    @Provides
    fun providePlayingAudioMessageServiceDependencies(
        dispatcherProvider: DispatcherProvider,
        audioMessagePlayer: ConversationAudioMessagePlayer,
    ): PlayingAudioMessageService.Dependencies =
        PlayingAudioMessageService.Dependencies(
            dispatcherProvider = dispatcherProvider,
            audioMessagePlayer = audioMessagePlayer,
        )

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
@Suppress("TooManyFunctions")
interface WireMetroHiltEntryPoint {
    @KaliumCoreLogic
    fun coreLogic(): CoreLogic

    fun userDataStoreProvider(): UserDataStoreProvider

    fun dispatcherProvider(): DispatcherProvider

    fun automatedLoginManager(): AutomatedLoginManager

    fun currentScreenManager(): CurrentScreenManager

    fun switchAccountObserver(): SwitchAccountObserver

    fun loginTypeSelector(): LoginTypeSelector

    fun proximitySensorManager(): ProximitySensorManager

    fun managedConfigurationsManager(): ManagedConfigurationsManager

    fun lockCodeTimeManager(): LockCodeTimeManager

    fun wireNotificationManager(): WireNotificationManager

    fun callNotificationManager(): CallNotificationManager

    fun conversationAudioMessagePlayer(): ConversationAudioMessagePlayer

    fun syncLifecycleManager(): SyncLifecycleManager

    fun wireWorkerFactory(): WireWorkerFactory

    fun globalObserversManager(): GlobalObserversManager

    @ApplicationScope
    fun applicationScope(): CoroutineScope

    fun accountSwitchUseCase(): AccountSwitchUseCase

    fun servicesManager(): ServicesManager
}
