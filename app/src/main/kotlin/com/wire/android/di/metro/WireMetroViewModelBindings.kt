@file:Suppress("TooManyFunctions")

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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.wire.android.ui.CallFeedbackViewModel
import com.wire.android.ui.debug.AiAssistantDebugViewModelImpl
import com.wire.android.ui.debug.DebugDataOptionsViewModelImpl
import com.wire.android.ui.debug.DebugInfoViewModelFactory
import com.wire.android.ui.debug.ExportObfuscatedCopyViewModelImpl
import com.wire.android.ui.debug.LogManagementViewModel
import com.wire.android.ui.debug.UserDebugViewModel
import com.wire.android.ui.debug.conversation.DebugConversationViewModel
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsViewModel
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModel
import com.wire.android.ui.MiscViewModelFactory
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.authentication.AuthenticationManualViewModelFactory
import com.wire.android.ui.authentication.AuthenticationViewModelFactory
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModel
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModel
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModel
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.calling.CallingManualViewModelFactory
import com.wire.android.ui.calling.CallingViewModelFactory
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.incoming.IncomingCallViewModel
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.outgoing.OutgoingCallViewModel
import com.wire.android.ui.common.CommonManualViewModelFactory
import com.wire.android.ui.common.CommonViewModelFactory
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentViewModel
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModel
import com.wire.android.ui.home.AppSyncViewModel
import com.wire.android.ui.home.HomeManualViewModelFactory
import com.wire.android.ui.home.HomeViewModel
import com.wire.android.ui.home.HomeViewModelFactory
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModel
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModel
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModel
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModel
import com.wire.android.ui.home.conversations.ConversationCoreManualViewModelFactory
import com.wire.android.ui.home.conversations.ConversationCoreViewModelFactory
import com.wire.android.ui.home.conversations.ConversationDetailsViewModelFactory
import com.wire.android.ui.home.conversations.ConversationSearchFolderManualViewModelFactory
import com.wire.android.ui.home.conversations.ConversationSearchFolderViewModelFactory
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModel
import com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModel
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModel
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVMImpl
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderArgs
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVMImpl
import com.wire.android.ui.home.conversations.folder.NewFolderViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel
import com.wire.android.ui.home.conversations.media.ConversationAssetMessagesViewModel
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewViewModel
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.QuotedMultipartMessageViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelImpl
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModelImpl
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModel
import com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModel
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesViewModel
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.conversations.ScopedMessageManualViewModelFactory
import com.wire.android.ui.home.conversations.ScopedMessageViewModelFactory
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelImpl
import com.wire.android.ui.home.conversationslist.ConversationListViewModelImpl
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.home.gallery.MediaGalleryViewModel
import com.wire.android.ui.home.messagecomposer.AiMessageComposerViewModel
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModel
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.settings.SettingsViewModel
import com.wire.android.ui.home.settings.SettingsViewModelFactory
import com.wire.android.ui.home.settings.account.MyAccountViewModel
import com.wire.android.ui.home.settings.account.color.ChangeUserColorViewModel
import com.wire.android.ui.home.settings.account.deleteAccount.DeleteAccountViewModel
import com.wire.android.ui.home.settings.account.displayname.ChangeDisplayNameViewModel
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailViewModel
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailViewModel
import com.wire.android.ui.home.settings.account.handle.ChangeHandleViewModel
import com.wire.android.ui.home.settings.appearance.CustomizationViewModel
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsViewModel
import com.wire.android.ui.home.settings.backup.BackupAndRestoreViewModel
import com.wire.android.ui.home.settings.about.dependencies.DependenciesViewModel
import com.wire.android.ui.home.settings.about.licenses.LicensesViewModel
import com.wire.android.ui.home.settings.privacy.PrivacySettingsViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.ui.home.whatsnew.WhatsNewViewModel
import com.wire.android.ui.initialsync.InitialSyncViewModel
import com.wire.android.ui.joinConversation.JoinConversationViaCodeViewModel
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.newauthentication.login.NewLoginViewModel
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModel
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModel
import com.wire.android.ui.settings.about.AboutThisAppViewModel
import com.wire.android.ui.settings.devices.DeviceDetailsViewModel
import com.wire.android.ui.settings.devices.SelfDevicesViewModel
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsViewModel
import com.wire.android.ui.sharing.ImportMediaAuthenticatedViewModel
import com.wire.android.ui.common.banner.SecurityClassificationArgs
import com.wire.android.ui.common.banner.SecurityClassificationViewModelImpl
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModelImpl
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.AudioMessageViewModelImpl
import com.wire.android.ui.home.conversations.CompositeMessageViewModelImpl
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuArgs
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuViewModelImpl
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathArgs
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathViewModelImpl
import com.wire.android.ui.home.conversations.model.CompositeMessageArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModelImpl
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelImpl
import com.wire.android.ui.home.messagecomposer.attachments.IsFileSharingEnabledViewModelImpl
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioViewModel
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModel
import com.wire.android.ui.userprofile.qr.SelfQRCodeViewModel
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel
import com.wire.android.ui.userprofile.service.ServiceDetailsViewModelImpl
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object WireMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelKey(AppSyncViewModel::class)
    fun appSyncViewModel(factory: HomeViewModelFactory): ViewModel =
        factory.appSyncViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(FeatureFlagNotificationViewModel::class)
    fun featureFlagNotificationViewModel(factory: HomeViewModelFactory): ViewModel =
        factory.featureFlagNotificationViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(HomeViewModel::class)
    fun homeViewModel(factory: HomeViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.homeViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(HomeDrawerViewModel::class)
    fun homeDrawerViewModel(factory: HomeViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.homeDrawerViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(NewConversationViewModel::class)
    fun newConversationViewModel(factory: HomeViewModelFactory): ViewModel =
        factory.newConversationViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(CallFeedbackViewModel::class)
    fun callFeedbackViewModel(factory: CallingViewModelFactory): ViewModel =
        factory.callFeedbackViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationCallViewModel::class)
    fun conversationCallViewModel(factory: CallingViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.conversationCallViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(ConversationListCallViewModelImpl::class)
    fun conversationListCallViewModel(factory: CallingViewModelFactory): ViewModel =
        factory.conversationListCallViewModel()

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(CallingManualViewModelFactory::class)
    fun callingManualViewModelFactory(factory: CallingViewModelFactory): ManualViewModelAssistedFactory =
        object : CallingManualViewModelFactory {
            override fun incomingCallViewModel(conversationId: ConversationId): IncomingCallViewModel =
                factory.incomingCallViewModel(conversationId)

            override fun outgoingCallViewModel(conversationId: ConversationId): OutgoingCallViewModel =
                factory.outgoingCallViewModel(conversationId)

            override fun ongoingCallViewModel(conversationId: ConversationId): OngoingCallViewModel =
                factory.ongoingCallViewModel(conversationId)

            override fun sharedCallingViewModel(conversationId: ConversationId): SharedCallingViewModel =
                factory.sharedCallingViewModel(conversationId)
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(HomeManualViewModelFactory::class)
    fun homeManualViewModelFactory(factory: HomeViewModelFactory): ManualViewModelAssistedFactory =
        object : HomeManualViewModelFactory {
            override fun conversationListViewModel(conversationsSource: ConversationsSource): ConversationListViewModelImpl =
                factory.conversationListViewModel(conversationsSource)
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(CommonManualViewModelFactory::class)
    fun commonManualViewModelFactory(factory: CommonViewModelFactory): ManualViewModelAssistedFactory =
        object : CommonManualViewModelFactory {
            override fun securityClassificationViewModel(args: SecurityClassificationArgs): SecurityClassificationViewModelImpl =
                factory.securityClassificationViewModel(args)

            override fun conversationOptionsMenuViewModel(): ConversationOptionsMenuViewModelImpl =
                factory.conversationOptionsMenuViewModel()
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ScopedMessageManualViewModelFactory::class)
    fun scopedMessageManualViewModelFactory(factory: ScopedMessageViewModelFactory): ManualViewModelAssistedFactory =
        object : ScopedMessageManualViewModelFactory {
            override fun compositeMessageViewModel(
                savedStateHandle: androidx.lifecycle.SavedStateHandle,
                args: CompositeMessageArgs
            ): CompositeMessageViewModelImpl =
                factory.compositeMessageViewModel(savedStateHandle, args)

            override fun messageOptionsMenuViewModel(args: MessageOptionsMenuArgs): MessageOptionsMenuViewModelImpl =
                factory.messageOptionsMenuViewModel(args)

            override fun typingIndicatorViewModel(args: TypingIndicatorArgs): TypingIndicatorViewModelImpl =
                factory.typingIndicatorViewModel(args)

            override fun assetLocalPathViewModel(args: AssetLocalPathArgs): AssetLocalPathViewModelImpl =
                factory.assetLocalPathViewModel(args)

            override fun selfDeletingMessageActionViewModel(
                args: SelfDeletingMessageActionArgs
            ): SelfDeletingMessageActionViewModelImpl =
                factory.selfDeletingMessageActionViewModel(args)

            override fun isFileSharingEnabledViewModel(): IsFileSharingEnabledViewModelImpl =
                factory.isFileSharingEnabledViewModel()

            override fun recordAudioViewModel(): RecordAudioViewModel =
                factory.recordAudioViewModel()

            override fun audioMessageViewModel(args: AudioMessageArgs): AudioMessageViewModelImpl =
                factory.audioMessageViewModel(args)
        }

    @Provides
    @IntoMap
    @ViewModelKey(UserDebugViewModel::class)
    fun userDebugViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.userDebugViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(LogManagementViewModel::class)
    fun logManagementViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.logManagementViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(DebugDataOptionsViewModelImpl::class)
    fun debugDataOptionsViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.debugDataOptionsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ExportObfuscatedCopyViewModelImpl::class)
    fun exportObfuscatedCopyViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.exportObfuscatedCopyViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(AiAssistantDebugViewModelImpl::class)
    fun aiAssistantDebugViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.aiAssistantDebugViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(DebugConversationViewModel::class)
    fun debugConversationViewModel(factory: DebugInfoViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.debugConversationViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(ConversationCryptoStatsViewModel::class)
    fun conversationCryptoStatsViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.conversationCryptoStatsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(DebugFeatureFlagsViewModel::class)
    fun debugFeatureFlagsViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.debugFeatureFlagsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(WhatsNewViewModel::class)
    fun whatsNewViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.whatsNewViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(AboutThisAppViewModel::class)
    fun aboutThisAppViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.aboutThisAppViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(DependenciesViewModel::class)
    fun dependenciesViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.dependenciesViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(LicensesViewModel::class)
    fun licensesViewModel(factory: DebugInfoViewModelFactory): ViewModel =
        factory.licensesViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(AnalyticsUsageViewModel::class)
    fun analyticsUsageViewModel(factory: MiscViewModelFactory): ViewModel =
        factory.analyticsUsageViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(InitialSyncViewModel::class)
    fun initialSyncViewModel(factory: MiscViewModelFactory): ViewModel =
        factory.initialSyncViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(LegalHoldRequestedViewModel::class)
    fun legalHoldRequestedViewModel(factory: MiscViewModelFactory): ViewModel =
        factory.legalHoldRequestedViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(E2EIEnrollmentViewModel::class)
    fun e2EIEnrollmentViewModel(factory: MiscViewModelFactory): ViewModel =
        factory.e2EIEnrollmentViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(GetE2EICertificateViewModel::class)
    fun getE2EICertificateViewModel(factory: MiscViewModelFactory): ViewModel =
        factory.getE2EICertificateViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(E2eiCertificateDetailsViewModel::class)
    fun e2eiCertificateDetailsViewModel(factory: MiscViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.e2eiCertificateDetailsViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(ImportMediaAuthenticatedViewModel::class)
    fun importMediaAuthenticatedViewModel(factory: MiscViewModelFactory): ViewModel =
        factory.importMediaAuthenticatedViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(JoinConversationViaCodeViewModel::class)
    fun joinConversationViaCodeViewModel(factory: MiscViewModelFactory): ViewModel =
        factory.joinConversationViaCodeViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(WelcomeViewModel::class)
    fun welcomeViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.welcomeViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(NewLoginViewModel::class)
    fun newLoginViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.newLoginViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(RegisterDeviceViewModel::class)
    fun registerDeviceViewModel(factory: AuthenticationViewModelFactory): ViewModel =
        factory.registerDeviceViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(RemoveDeviceViewModel::class)
    fun removeDeviceViewModel(factory: AuthenticationViewModelFactory): ViewModel =
        factory.removeDeviceViewModel()

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(AuthenticationManualViewModelFactory::class)
    fun authenticationManualViewModelFactory(factory: AuthenticationViewModelFactory): ManualViewModelAssistedFactory =
        object : AuthenticationManualViewModelFactory {
            override fun loginEmailViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginEmailViewModel =
                factory.loginEmailViewModel(loginNavArgs, extras.createSavedStateHandle())

            override fun loginSSOViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginSSOViewModel =
                factory.loginSSOViewModel(loginNavArgs, extras.createSavedStateHandle())

            override fun clearSessionViewModel(cancelUserId: UserId?): ClearSessionViewModel =
                factory.clearSessionViewModel(cancelUserId)
        }

    @Provides
    @IntoMap
    @ViewModelKey(CreateAccountUsernameViewModel::class)
    fun createAccountUsernameViewModel(factory: AuthenticationViewModelFactory): ViewModel =
        factory.createAccountUsernameViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountOverviewViewModel::class)
    fun createAccountOverviewViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountOverviewViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountEmailViewModel::class)
    fun createAccountEmailViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountEmailViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountDetailsViewModel::class)
    fun createAccountDetailsViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountDetailsViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountCodeViewModel::class)
    fun createAccountCodeViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountCodeViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountSummaryViewModel::class)
    fun createAccountSummaryViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountSummaryViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountSelectorViewModel::class)
    fun createAccountSelectorViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountSelectorViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountDataDetailViewModel::class)
    fun createAccountDataDetailViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountDataDetailViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountVerificationCodeViewModel::class)
    fun createAccountVerificationCodeViewModel(factory: AuthenticationViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createAccountVerificationCodeViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationMessagesViewModel::class)
    fun conversationMessagesViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.conversationMessagesViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageComposerViewModel::class)
    fun messageComposerViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.messageComposerViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(SendMessageViewModel::class)
    fun sendMessageViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.sendMessageViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageDraftViewModel::class)
    fun messageDraftViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.messageDraftViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageAttachmentsViewModel::class)
    fun messageAttachmentsViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.messageAttachmentsViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationMigrationViewModel::class)
    fun conversationMigrationViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.conversationMigrationViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(ConversationAssetPathsViewModelImpl::class)
    fun conversationAssetPathsViewModel(factory: ConversationCoreViewModelFactory): ViewModel =
        factory.conversationAssetPathsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(AiMessageComposerViewModel::class)
    fun aiMessageComposerViewModel(factory: ConversationCoreViewModelFactory): ViewModel =
        factory.aiMessageComposerViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MediaGalleryViewModel::class)
    fun mediaGalleryViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.mediaGalleryViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(LocationPickerViewModel::class)
    fun locationPickerViewModel(factory: ConversationCoreViewModelFactory): ViewModel =
        factory.locationPickerViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationAssetMessagesViewModel::class)
    fun conversationAssetMessagesViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.conversationAssetMessagesViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ImagesPreviewViewModel::class)
    fun imagesPreviewViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.imagesPreviewViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageDetailsViewModel::class)
    fun messageDetailsViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.messageDetailsViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(QuotedMultipartMessageViewModel::class)
    fun quotedMultipartMessageViewModel(factory: ConversationCoreViewModelFactory): ViewModel =
        factory.quotedMultipartMessageViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationBannerViewModel::class)
    fun conversationBannerViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.conversationBannerViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationInfoViewModel::class)
    fun conversationInfoViewModel(factory: ConversationCoreViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.conversationInfoViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ConversationCoreManualViewModelFactory::class)
    fun conversationCoreManualViewModelFactory(
        factory: ConversationCoreViewModelFactory
    ): ManualViewModelAssistedFactory =
        object : ConversationCoreManualViewModelFactory {
            override fun multipartAttachmentsViewModel(conversationId: ConversationId): MultipartAttachmentsViewModelImpl =
                factory.multipartAttachmentsViewModel(conversationId)
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(GroupConversationDetailsViewModel::class)
    fun groupConversationDetailsViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.groupConversationDetailsViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(GroupConversationParticipantsViewModel::class)
    fun groupConversationParticipantsViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.groupConversationParticipantsViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(EditConversationMetadataViewModel::class)
    fun editConversationMetadataViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.editConversationMetadataViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(EditSelfDeletingMessagesViewModel::class)
    fun editSelfDeletingMessagesViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.editSelfDeletingMessagesViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(UpdateChannelAccessViewModel::class)
    fun updateChannelAccessViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.updateChannelAccessViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(UpdateAppsAccessViewModel::class)
    fun updateAppsAccessViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.updateAppsAccessViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(EditGuestAccessViewModel::class)
    fun editGuestAccessViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.editGuestAccessViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreatePasswordGuestLinkViewModel::class)
    fun createPasswordGuestLinkViewModel(factory: ConversationDetailsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.createPasswordGuestLinkViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(CheckAssetRestrictionsViewModel::class)
    fun checkAssetRestrictionsViewModel(factory: ConversationDetailsViewModelFactory): ViewModel =
        factory.checkAssetRestrictionsViewModel()

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ConversationSearchFolderManualViewModelFactory::class)
    fun conversationSearchFolderManualViewModelFactory(
        factory: ConversationSearchFolderViewModelFactory
    ): ManualViewModelAssistedFactory =
        object : ConversationSearchFolderManualViewModelFactory {
            override fun conversationFoldersViewModel(args: ConversationFoldersStateArgs): ConversationFoldersVMImpl =
                factory.conversationFoldersViewModel(args)

            override fun moveConversationToFolderViewModel(args: MoveConversationToFolderArgs): MoveConversationToFolderVMImpl =
                factory.moveConversationToFolderViewModel(args)
        }

    @Provides
    @IntoMap
    @ViewModelKey(NewFolderViewModel::class)
    fun newFolderViewModel(factory: ConversationSearchFolderViewModelFactory): ViewModel =
        factory.newFolderViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(AddMembersToConversationViewModel::class)
    fun addMembersToConversationViewModel(factory: ConversationSearchFolderViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.addMembersToConversationViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(SearchConversationMessagesViewModel::class)
    fun searchConversationMessagesViewModel(factory: ConversationSearchFolderViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.searchConversationMessagesViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(PromoteAdminViewModel::class)
    fun promoteAdminViewModel(factory: ConversationSearchFolderViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.promoteAdminViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    fun settingsViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.settingsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(MyAccountViewModel::class)
    fun myAccountViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.myAccountViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(DeleteAccountViewModel::class)
    fun deleteAccountViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.deleteAccountViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ChangeDisplayNameViewModel::class)
    fun changeDisplayNameViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.changeDisplayNameViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ChangeUserColorViewModel::class)
    fun changeUserColorViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.changeUserColorViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ChangeEmailViewModel::class)
    fun changeEmailViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.changeEmailViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ChangeHandleViewModel::class)
    fun changeHandleViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.changeHandleViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(CustomizationViewModel::class)
    fun customizationViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.customizationViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(NetworkSettingsViewModel::class)
    fun networkSettingsViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.networkSettingsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(PrivacySettingsViewModel::class)
    fun privacySettingsViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.privacySettingsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(BackupAndRestoreViewModel::class)
    fun backupAndRestoreViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.backupAndRestoreViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(SetLockScreenViewModel::class)
    fun setLockScreenViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.setLockScreenViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ForgotLockScreenViewModel::class)
    fun forgotLockScreenViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.forgotLockScreenViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(AppUnlockWithBiometricsViewModel::class)
    fun appUnlockWithBiometricsViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.appUnlockWithBiometricsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(EnterLockScreenViewModel::class)
    fun enterLockScreenViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.enterLockScreenViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(SelfDevicesViewModel::class)
    fun selfDevicesViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.selfDevicesViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(AvatarPickerViewModel::class)
    fun avatarPickerViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.avatarPickerViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(SelfUserProfileViewModel::class)
    fun selfUserProfileViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.selfUserProfileViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(TeamMigrationViewModel::class)
    fun teamMigrationViewModel(factory: SettingsViewModelFactory): ViewModel =
        factory.teamMigrationViewModel()

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(VerifyEmailViewModel::class)
    fun verifyEmailViewModel(factory: SettingsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.verifyEmailViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(DeviceDetailsViewModel::class)
    fun deviceDetailsViewModel(factory: SettingsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.deviceDetailsViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(SelfQRCodeViewModel::class)
    fun selfQRCodeViewModel(factory: SettingsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.selfQRCodeViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(OtherUserProfileScreenViewModel::class)
    fun otherUserProfileScreenViewModel(factory: SettingsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.otherUserProfileScreenViewModel(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ServiceDetailsViewModelImpl::class)
    fun serviceDetailsViewModel(factory: SettingsViewModelFactory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.serviceDetailsViewModel(extras.createSavedStateHandle())
        }
}
