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
import com.wire.android.ui.debug.DebugDataOptionsViewModelImpl
import com.wire.android.ui.debug.ExportObfuscatedCopyViewModelImpl
import com.wire.android.ui.debug.LogManagementViewModel
import com.wire.android.ui.debug.UserDebugViewModel
import com.wire.android.ui.debug.conversation.DebugConversationViewModel
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsViewModel
import com.wire.android.ui.debug.experimental.DebugExperimentalFeaturesViewModel
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModel
import com.wire.android.ui.debug.securityproviders.SecurityProvidersViewModel
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.authentication.AuthenticationManualViewModelFactory
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
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.incoming.IncomingCallViewModel
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.outgoing.OutgoingCallViewModel
import com.wire.android.ui.common.CommonManualViewModelFactory
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentViewModel
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModel
import com.wire.android.ui.home.AppSyncViewModel
import com.wire.android.ui.home.HomeManualViewModelFactory
import com.wire.android.ui.home.HomeViewModel
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModel
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModel
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModel
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModel
import com.wire.android.ui.home.conversations.ConversationCoreManualViewModelFactory
import com.wire.android.ui.home.conversations.ConversationSearchFolderManualViewModelFactory
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
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelImpl
import com.wire.android.ui.home.conversationslist.ConversationListViewModelImpl
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.home.gallery.MediaGalleryViewModel
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModel
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.settings.SettingsViewModel
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
import com.wire.android.ui.home.meetings.MeetingsCallViewModel
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
import dev.zacsweers.metro.Provider
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
    fun appSyncViewModel(viewModel: AppSyncViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(FeatureFlagNotificationViewModel::class)
    fun featureFlagNotificationViewModel(viewModel: FeatureFlagNotificationViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(HomeViewModel::class)
    fun homeViewModel(factory: HomeViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(HomeDrawerViewModel::class)
    fun homeDrawerViewModel(factory: HomeDrawerViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(NewConversationViewModel::class)
    fun newConversationViewModel(viewModel: NewConversationViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(CallFeedbackViewModel::class)
    fun callFeedbackViewModel(viewModel: CallFeedbackViewModel): ViewModel =
        viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationCallViewModel::class)
    fun conversationCallViewModel(factory: ConversationCallViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(MeetingsCallViewModel::class)
    fun joinOrStartCallViewModel(viewModel: MeetingsCallViewModel): ViewModel =
        viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ConversationListCallViewModelImpl::class)
    fun conversationListCallViewModel(viewModel: ConversationListCallViewModelImpl): ViewModel =
        viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(CallingManualViewModelFactory::class)
    fun callingManualViewModelFactory(
        incomingCallFactory: IncomingCallViewModel.Factory,
        outgoingCallFactory: OutgoingCallViewModel.Factory,
        ongoingCallFactory: OngoingCallViewModel.Factory,
        sharedCallingFactory: SharedCallingViewModel.Factory,
    ): ManualViewModelAssistedFactory =
        object : CallingManualViewModelFactory {
            override fun incomingCallViewModel(conversationId: ConversationId): IncomingCallViewModel =
                incomingCallFactory.create(conversationId)

            override fun outgoingCallViewModel(conversationId: ConversationId): OutgoingCallViewModel =
                outgoingCallFactory.create(conversationId)

            override fun ongoingCallViewModel(conversationId: ConversationId): OngoingCallViewModel =
                ongoingCallFactory.create(conversationId)

            override fun sharedCallingViewModel(conversationId: ConversationId): SharedCallingViewModel =
                sharedCallingFactory.create(conversationId)
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(HomeManualViewModelFactory::class)
    fun homeManualViewModelFactory(factory: ConversationListViewModelImpl.Factory): ManualViewModelAssistedFactory =
        object : HomeManualViewModelFactory {
            override fun conversationListViewModel(conversationsSource: ConversationsSource): ConversationListViewModelImpl =
                factory.create(conversationsSource)
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(CommonManualViewModelFactory::class)
    fun commonManualViewModelFactory(
        securityClassificationFactory: SecurityClassificationViewModelImpl.Factory,
        conversationOptionsMenuProvider: Provider<ConversationOptionsMenuViewModelImpl>,
    ): ManualViewModelAssistedFactory =
        object : CommonManualViewModelFactory {
            override fun securityClassificationViewModel(args: SecurityClassificationArgs): SecurityClassificationViewModelImpl =
                securityClassificationFactory.create(args)

            override fun conversationOptionsMenuViewModel(): ConversationOptionsMenuViewModelImpl =
                conversationOptionsMenuProvider()
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ScopedMessageManualViewModelFactory::class)
    @Suppress("LongParameterList")
    internal fun scopedMessageManualViewModelFactory(
        compositeMessageFactory: CompositeMessageViewModelImpl.Factory,
        messageOptionsMenuFactory: MessageOptionsMenuViewModelImpl.Factory,
        typingIndicatorFactory: TypingIndicatorViewModelImpl.Factory,
        assetLocalPathFactory: AssetLocalPathViewModelImpl.Factory,
        selfDeletingMessageActionFactory: SelfDeletingMessageActionViewModelImpl.Factory,
        isFileSharingEnabledProvider: Provider<IsFileSharingEnabledViewModelImpl>,
        recordAudioProvider: Provider<RecordAudioViewModel>,
        audioMessageFactory: AudioMessageViewModelImpl.Factory,
    ): ManualViewModelAssistedFactory =
        object : ScopedMessageManualViewModelFactory {
            override fun compositeMessageViewModel(
                savedStateHandle: androidx.lifecycle.SavedStateHandle,
                args: CompositeMessageArgs
            ): CompositeMessageViewModelImpl =
                compositeMessageFactory.create(savedStateHandle, args)

            override fun messageOptionsMenuViewModel(args: MessageOptionsMenuArgs): MessageOptionsMenuViewModelImpl =
                messageOptionsMenuFactory.create(args)

            override fun typingIndicatorViewModel(args: TypingIndicatorArgs): TypingIndicatorViewModelImpl =
                typingIndicatorFactory.create(args)

            override fun assetLocalPathViewModel(args: AssetLocalPathArgs): AssetLocalPathViewModelImpl =
                assetLocalPathFactory.create(args)

            override fun selfDeletingMessageActionViewModel(
                args: SelfDeletingMessageActionArgs
            ): SelfDeletingMessageActionViewModelImpl =
                selfDeletingMessageActionFactory.create(args)

            override fun isFileSharingEnabledViewModel(): IsFileSharingEnabledViewModelImpl =
                isFileSharingEnabledProvider()

            override fun recordAudioViewModel(): RecordAudioViewModel =
                recordAudioProvider()

            override fun audioMessageViewModel(args: AudioMessageArgs): AudioMessageViewModelImpl =
                audioMessageFactory.create(args)
        }

    @Provides
    @IntoMap
    @ViewModelKey(UserDebugViewModel::class)
    fun userDebugViewModel(viewModel: UserDebugViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(LogManagementViewModel::class)
    fun logManagementViewModel(viewModel: LogManagementViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(DebugDataOptionsViewModelImpl::class)
    fun debugDataOptionsViewModel(viewModel: DebugDataOptionsViewModelImpl): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ExportObfuscatedCopyViewModelImpl::class)
    fun exportObfuscatedCopyViewModel(viewModel: ExportObfuscatedCopyViewModelImpl): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(DebugConversationViewModel::class)
    fun debugConversationViewModel(factory: DebugConversationViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(ConversationCryptoStatsViewModel::class)
    fun conversationCryptoStatsViewModel(viewModel: ConversationCryptoStatsViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(DebugFeatureFlagsViewModel::class)
    fun debugFeatureFlagsViewModel(viewModel: DebugFeatureFlagsViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(DebugExperimentalFeaturesViewModel::class)
    fun debugExperimentalFeaturesViewModel(viewModel: DebugExperimentalFeaturesViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(SecurityProvidersViewModel::class)
    fun securityProvidersViewModel(viewModel: SecurityProvidersViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(WhatsNewViewModel::class)
    fun whatsNewViewModel(viewModel: WhatsNewViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(AboutThisAppViewModel::class)
    fun aboutThisAppViewModel(viewModel: AboutThisAppViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(DependenciesViewModel::class)
    fun dependenciesViewModel(viewModel: DependenciesViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(LicensesViewModel::class)
    fun licensesViewModel(viewModel: LicensesViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(AnalyticsUsageViewModel::class)
    fun analyticsUsageViewModel(viewModel: AnalyticsUsageViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(InitialSyncViewModel::class)
    fun initialSyncViewModel(viewModel: InitialSyncViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(LegalHoldRequestedViewModel::class)
    fun legalHoldRequestedViewModel(viewModel: LegalHoldRequestedViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(E2EIEnrollmentViewModel::class)
    fun e2EIEnrollmentViewModel(viewModel: E2EIEnrollmentViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(GetE2EICertificateViewModel::class)
    fun getE2EICertificateViewModel(viewModel: GetE2EICertificateViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(E2eiCertificateDetailsViewModel::class)
    fun e2eiCertificateDetailsViewModel(factory: E2eiCertificateDetailsViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(ImportMediaAuthenticatedViewModel::class)
    fun importMediaAuthenticatedViewModel(viewModel: ImportMediaAuthenticatedViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(JoinConversationViaCodeViewModel::class)
    fun joinConversationViaCodeViewModel(viewModel: JoinConversationViaCodeViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(WelcomeViewModel::class)
    fun welcomeViewModel(factory: WelcomeViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(NewLoginViewModel::class)
    fun newLoginViewModel(factory: NewLoginViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(RegisterDeviceViewModel::class)
    fun registerDeviceViewModel(viewModel: RegisterDeviceViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(RemoveDeviceViewModel::class)
    fun removeDeviceViewModel(viewModel: RemoveDeviceViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(AuthenticationManualViewModelFactory::class)
    fun authenticationManualViewModelFactory(
        loginEmailFactory: LoginEmailViewModel.Factory,
        loginSSOFactory: LoginSSOViewModel.Factory,
        clearSessionFactory: ClearSessionViewModel.Factory,
    ): ManualViewModelAssistedFactory =
        object : AuthenticationManualViewModelFactory {
            override fun loginEmailViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginEmailViewModel =
                loginEmailFactory.create(loginNavArgs, extras.createSavedStateHandle())

            override fun loginSSOViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginSSOViewModel =
                loginSSOFactory.create(loginNavArgs, extras.createSavedStateHandle())

            override fun clearSessionViewModel(cancelUserId: UserId?): ClearSessionViewModel =
                clearSessionFactory.create(cancelUserId)
        }

    @Provides
    @IntoMap
    @ViewModelKey(CreateAccountUsernameViewModel::class)
    fun createAccountUsernameViewModel(viewModel: CreateAccountUsernameViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountOverviewViewModel::class)
    fun createAccountOverviewViewModel(factory: CreateAccountOverviewViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountEmailViewModel::class)
    fun createAccountEmailViewModel(factory: CreateAccountEmailViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountDetailsViewModel::class)
    fun createAccountDetailsViewModel(factory: CreateAccountDetailsViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountCodeViewModel::class)
    fun createAccountCodeViewModel(factory: CreateAccountCodeViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountSummaryViewModel::class)
    fun createAccountSummaryViewModel(factory: CreateAccountSummaryViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountSelectorViewModel::class)
    fun createAccountSelectorViewModel(factory: CreateAccountSelectorViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountDataDetailViewModel::class)
    fun createAccountDataDetailViewModel(factory: CreateAccountDataDetailViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreateAccountVerificationCodeViewModel::class)
    fun createAccountVerificationCodeViewModel(factory: CreateAccountVerificationCodeViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationMessagesViewModel::class)
    fun conversationMessagesViewModel(factory: ConversationMessagesViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageComposerViewModel::class)
    fun messageComposerViewModel(factory: MessageComposerViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(SendMessageViewModel::class)
    fun sendMessageViewModel(factory: SendMessageViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageDraftViewModel::class)
    fun messageDraftViewModel(factory: MessageDraftViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageAttachmentsViewModel::class)
    fun messageAttachmentsViewModel(factory: MessageAttachmentsViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationMigrationViewModel::class)
    fun conversationMigrationViewModel(factory: ConversationMigrationViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(ConversationAssetPathsViewModelImpl::class)
    fun conversationAssetPathsViewModel(viewModel: ConversationAssetPathsViewModelImpl): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MediaGalleryViewModel::class)
    fun mediaGalleryViewModel(factory: MediaGalleryViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(LocationPickerViewModel::class)
    fun locationPickerViewModel(viewModel: LocationPickerViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationAssetMessagesViewModel::class)
    fun conversationAssetMessagesViewModel(factory: ConversationAssetMessagesViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ImagesPreviewViewModel::class)
    fun imagesPreviewViewModel(factory: ImagesPreviewViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(MessageDetailsViewModel::class)
    fun messageDetailsViewModel(factory: MessageDetailsViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(QuotedMultipartMessageViewModel::class)
    fun quotedMultipartMessageViewModel(viewModel: QuotedMultipartMessageViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationBannerViewModel::class)
    fun conversationBannerViewModel(factory: ConversationBannerViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ConversationInfoViewModel::class)
    fun conversationInfoViewModel(factory: ConversationInfoViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ConversationCoreManualViewModelFactory::class)
    fun conversationCoreManualViewModelFactory(
        factory: MultipartAttachmentsViewModelImpl.Factory
    ): ManualViewModelAssistedFactory =
        object : ConversationCoreManualViewModelFactory {
            override fun multipartAttachmentsViewModel(conversationId: ConversationId): MultipartAttachmentsViewModelImpl =
                factory.create(conversationId)
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(GroupConversationDetailsViewModel::class)
    fun groupConversationDetailsViewModel(factory: GroupConversationDetailsViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(GroupConversationParticipantsViewModel::class)
    fun groupConversationParticipantsViewModel(factory: GroupConversationParticipantsViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(EditConversationMetadataViewModel::class)
    fun editConversationMetadataViewModel(factory: EditConversationMetadataViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(EditSelfDeletingMessagesViewModel::class)
    fun editSelfDeletingMessagesViewModel(factory: EditSelfDeletingMessagesViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(UpdateChannelAccessViewModel::class)
    fun updateChannelAccessViewModel(factory: UpdateChannelAccessViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(UpdateAppsAccessViewModel::class)
    fun updateAppsAccessViewModel(factory: UpdateAppsAccessViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(EditGuestAccessViewModel::class)
    fun editGuestAccessViewModel(factory: EditGuestAccessViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(CreatePasswordGuestLinkViewModel::class)
    fun createPasswordGuestLinkViewModel(factory: CreatePasswordGuestLinkViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(CheckAssetRestrictionsViewModel::class)
    fun checkAssetRestrictionsViewModel(viewModel: CheckAssetRestrictionsViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ConversationSearchFolderManualViewModelFactory::class)
    fun conversationSearchFolderManualViewModelFactory(
        conversationFoldersFactory: ConversationFoldersVMImpl.Factory,
        moveConversationToFolderFactory: MoveConversationToFolderVMImpl.Factory,
    ): ManualViewModelAssistedFactory =
        object : ConversationSearchFolderManualViewModelFactory {
            override fun conversationFoldersViewModel(args: ConversationFoldersStateArgs): ConversationFoldersVMImpl =
                conversationFoldersFactory.create(args)

            override fun moveConversationToFolderViewModel(args: MoveConversationToFolderArgs): MoveConversationToFolderVMImpl =
                moveConversationToFolderFactory.create(args)
        }

    @Provides
    @IntoMap
    @ViewModelKey(NewFolderViewModel::class)
    fun newFolderViewModel(viewModel: NewFolderViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(AddMembersToConversationViewModel::class)
    fun addMembersToConversationViewModel(factory: AddMembersToConversationViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(SearchConversationMessagesViewModel::class)
    fun searchConversationMessagesViewModel(factory: SearchConversationMessagesViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(PromoteAdminViewModel::class)
    fun promoteAdminViewModel(factory: PromoteAdminViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    fun settingsViewModel(viewModel: SettingsViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(MyAccountViewModel::class)
    fun myAccountViewModel(viewModel: MyAccountViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(DeleteAccountViewModel::class)
    fun deleteAccountViewModel(viewModel: DeleteAccountViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ChangeDisplayNameViewModel::class)
    fun changeDisplayNameViewModel(viewModel: ChangeDisplayNameViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ChangeUserColorViewModel::class)
    fun changeUserColorViewModel(viewModel: ChangeUserColorViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ChangeEmailViewModel::class)
    fun changeEmailViewModel(viewModel: ChangeEmailViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ChangeHandleViewModel::class)
    fun changeHandleViewModel(viewModel: ChangeHandleViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(CustomizationViewModel::class)
    fun customizationViewModel(viewModel: CustomizationViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(NetworkSettingsViewModel::class)
    fun networkSettingsViewModel(viewModel: NetworkSettingsViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(PrivacySettingsViewModel::class)
    fun privacySettingsViewModel(viewModel: PrivacySettingsViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(BackupAndRestoreViewModel::class)
    fun backupAndRestoreViewModel(viewModel: BackupAndRestoreViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(SetLockScreenViewModel::class)
    fun setLockScreenViewModel(viewModel: SetLockScreenViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(ForgotLockScreenViewModel::class)
    fun forgotLockScreenViewModel(viewModel: ForgotLockScreenViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(AppUnlockWithBiometricsViewModel::class)
    fun appUnlockWithBiometricsViewModel(viewModel: AppUnlockWithBiometricsViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(EnterLockScreenViewModel::class)
    fun enterLockScreenViewModel(viewModel: EnterLockScreenViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(SelfDevicesViewModel::class)
    fun selfDevicesViewModel(viewModel: SelfDevicesViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(AvatarPickerViewModel::class)
    fun avatarPickerViewModel(viewModel: AvatarPickerViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(SelfUserProfileViewModel::class)
    fun selfUserProfileViewModel(viewModel: SelfUserProfileViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(TeamMigrationViewModel::class)
    fun teamMigrationViewModel(viewModel: TeamMigrationViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(VerifyEmailViewModel::class)
    fun verifyEmailViewModel(factory: VerifyEmailViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(DeviceDetailsViewModel::class)
    fun deviceDetailsViewModel(factory: DeviceDetailsViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(SelfQRCodeViewModel::class)
    fun selfQRCodeViewModel(factory: SelfQRCodeViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(OtherUserProfileScreenViewModel::class)
    fun otherUserProfileScreenViewModel(factory: OtherUserProfileScreenViewModel.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ServiceDetailsViewModelImpl::class)
    fun serviceDetailsViewModel(factory: ServiceDetailsViewModelImpl.Factory): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                factory.create(extras.createSavedStateHandle())
        }
}
