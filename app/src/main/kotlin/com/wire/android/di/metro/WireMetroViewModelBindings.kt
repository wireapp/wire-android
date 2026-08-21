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
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.authentication.SessionAuthenticationManualViewModelFactory
import com.wire.android.ui.authentication.SessionAuthenticationViewModelFactory
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModel
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentViewModel
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModel
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModel
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModel
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModel
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModel
import com.wire.android.ui.home.conversations.ConversationCoreManualViewModelFactory
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.ConversationDetailsManualViewModelFactory
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavArgs
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessNavArgs
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkNavArgs
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesNavArgs
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModel
import com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel
import com.wire.android.ui.home.conversations.details.metadata.EditConversationNameNavArgs
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsNavArgs
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessNavArgs
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModel
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessArgs
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModel
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel
import com.wire.android.ui.home.conversations.media.ConversationAssetMessagesViewModel
import com.wire.android.ui.home.conversations.media.ConversationMediaNavArgs
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavArgs
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewViewModel
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsNavArgs
import com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.QuotedMultipartMessageViewModel
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.ConversationAssetPathsViewModelImpl
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.MultipartAttachmentsViewModelImpl
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.conversations.ScopedMessageManualViewModelFactory
import com.wire.android.ui.home.gallery.MediaGalleryNavArgs
import com.wire.android.ui.home.gallery.MediaGalleryViewModel
import com.wire.android.ui.home.messagecomposer.location.LocationPickerViewModel
import com.wire.android.ui.home.settings.SettingsViewModel
import com.wire.android.ui.home.settings.SettingsManualViewModelFactory
import com.wire.android.ui.home.settings.account.MyAccountViewModel
import com.wire.android.ui.home.settings.account.color.ChangeUserColorViewModel
import com.wire.android.ui.home.settings.account.deleteAccount.DeleteAccountViewModel
import com.wire.android.ui.home.settings.account.displayname.ChangeDisplayNameViewModel
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailViewModel
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailViewModel
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailViewModelArgs
import com.wire.android.ui.home.settings.account.handle.ChangeHandleViewModel
import com.wire.android.ui.home.settings.appearance.CustomizationViewModel
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsViewModel
import com.wire.android.ui.home.settings.backup.BackupAndRestoreViewModel
import com.wire.android.ui.home.settings.privacy.PrivacySettingsViewModel
import com.wire.android.ui.initialsync.InitialSyncViewModel
import com.wire.android.ui.joinConversation.JoinConversationViaCodeViewModel
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedViewModel
import com.wire.android.ui.settings.devices.SelfDevicesViewModel
import com.wire.android.ui.settings.devices.DeviceDetailsViewModel
import com.wire.android.ui.settings.devices.DeviceDetailsViewModelArgs
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsViewModel
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsViewModelArgs
import com.wire.android.ui.sharing.ImportMediaAuthenticatedViewModel
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
import com.wire.android.ui.userprofile.other.OtherUserProfileViewModelArgs
import com.wire.android.ui.userprofile.qr.SelfQRCodeViewModel
import com.wire.android.ui.userprofile.qr.SelfQrCodeViewModelArgs
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel
import com.wire.android.ui.userprofile.service.ServiceDetailsViewModelArgs
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
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object WireMetroViewModelBindings {

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
                args: CompositeMessageArgs
            ): CompositeMessageViewModelImpl =
                compositeMessageFactory.create(args)

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
    @ViewModelKey(LegalHoldDeactivatedViewModel::class)
    fun legalHoldDeactivatedViewModel(viewModel: LegalHoldDeactivatedViewModel): ViewModel = viewModel

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
    @ViewModelKey(ImportMediaAuthenticatedViewModel::class)
    fun importMediaAuthenticatedViewModel(viewModel: ImportMediaAuthenticatedViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(JoinConversationViaCodeViewModel::class)
    fun joinConversationViaCodeViewModel(viewModel: JoinConversationViaCodeViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(RegisterDeviceViewModel::class)
    fun registerDeviceViewModel(factory: SessionAuthenticationViewModelFactory): ViewModel =
        factory.registerDeviceViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(RemoveDeviceViewModel::class)
    fun removeDeviceViewModel(factory: SessionAuthenticationViewModelFactory): ViewModel =
        factory.removeDeviceViewModel()

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(SessionAuthenticationManualViewModelFactory::class)
    fun sessionAuthenticationManualViewModelFactory(
        factory: SessionAuthenticationViewModelFactory,
    ): ManualViewModelAssistedFactory = object : SessionAuthenticationManualViewModelFactory {
        override fun clearSessionViewModel(cancelUserId: UserId?): ClearSessionViewModel =
            factory.clearSessionViewModel(cancelUserId)
    }

    @Provides
    @IntoMap
    @ViewModelKey(CreateAccountUsernameViewModel::class)
    fun createAccountUsernameViewModel(factory: SessionAuthenticationViewModelFactory): ViewModel =
        factory.createAccountUsernameViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ConversationAssetPathsViewModelImpl::class)
    fun conversationAssetPathsViewModel(viewModel: ConversationAssetPathsViewModelImpl): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(LocationPickerViewModel::class)
    fun locationPickerViewModel(viewModel: LocationPickerViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(QuotedMultipartMessageViewModel::class)
    fun quotedMultipartMessageViewModel(viewModel: QuotedMultipartMessageViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ConversationCoreManualViewModelFactory::class)
    @Suppress("LongParameterList")
    fun conversationCoreManualViewModelFactory(
        conversationMessagesFactory: ConversationMessagesViewModel.Factory,
        messageComposerFactory: MessageComposerViewModel.Factory,
        sendMessageFactory: SendMessageViewModel.Factory,
        messageDraftFactory: MessageDraftViewModel.Factory,
        messageAttachmentsFactory: MessageAttachmentsViewModel.Factory,
        conversationMigrationFactory: ConversationMigrationViewModel.Factory,
        conversationBannerFactory: ConversationBannerViewModel.Factory,
        conversationInfoFactory: com.wire.android.ui.home.conversations.info.ConversationInfoViewModel.Factory,
        multipartAttachmentsFactory: MultipartAttachmentsViewModelImpl.Factory,
        mediaGalleryFactory: MediaGalleryViewModel.Factory,
        conversationAssetMessagesFactory: ConversationAssetMessagesViewModel.Factory,
        imagesPreviewFactory: ImagesPreviewViewModel.Factory,
        messageDetailsFactory: MessageDetailsViewModel.Factory,
    ): ManualViewModelAssistedFactory =
        object : ConversationCoreManualViewModelFactory {
            override fun conversationMessagesViewModel(args: ConversationNavArgs) =
                conversationMessagesFactory.create(args)
            override fun messageComposerViewModel(args: ConversationNavArgs) =
                messageComposerFactory.create(args)
            override fun sendMessageViewModel(args: ConversationNavArgs) =
                sendMessageFactory.create(args)
            override fun messageDraftViewModel(args: ConversationNavArgs) =
                messageDraftFactory.create(args)
            override fun messageAttachmentsViewModel(args: ConversationNavArgs) =
                messageAttachmentsFactory.create(args)
            override fun conversationMigrationViewModel(args: ConversationNavArgs) =
                conversationMigrationFactory.create(args)
            override fun conversationBannerViewModel(args: ConversationNavArgs) =
                conversationBannerFactory.create(args)
            override fun conversationInfoViewModel(args: ConversationNavArgs) =
                conversationInfoFactory.create(args)
            override fun multipartAttachmentsViewModel(conversationId: ConversationId): MultipartAttachmentsViewModelImpl =
                multipartAttachmentsFactory.create(conversationId)
            override fun mediaGalleryViewModel(args: MediaGalleryNavArgs) = mediaGalleryFactory.create(args)
            override fun conversationAssetMessagesViewModel(args: ConversationMediaNavArgs) =
                conversationAssetMessagesFactory.create(args)
            override fun imagesPreviewViewModel(args: ImagesPreviewNavArgs) = imagesPreviewFactory.create(args)
            override fun messageDetailsViewModel(args: MessageDetailsNavArgs) = messageDetailsFactory.create(args)
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ConversationDetailsManualViewModelFactory::class)
    @Suppress("LongParameterList")
    fun conversationDetailsManualViewModelFactory(
        groupDetailsFactory: GroupConversationDetailsViewModel.Factory,
        participantsFactory: GroupConversationParticipantsViewModel.Factory,
        metadataFactory: EditConversationMetadataViewModel.Factory,
        selfDeletingFactory: EditSelfDeletingMessagesViewModel.Factory,
        channelAccessFactory: UpdateChannelAccessViewModel.Factory,
        appsAccessFactory: UpdateAppsAccessViewModel.Factory,
        guestAccessFactory: EditGuestAccessViewModel.Factory,
        guestLinkFactory: CreatePasswordGuestLinkViewModel.Factory,
    ): ManualViewModelAssistedFactory = object : ConversationDetailsManualViewModelFactory {
        override fun groupConversationDetailsViewModel(args: GroupConversationDetailsNavArgs) =
            groupDetailsFactory.create(args)
        override fun groupConversationParticipantsViewModel(args: GroupConversationAllParticipantsNavArgs) =
            participantsFactory.create(args)
        override fun editConversationMetadataViewModel(args: EditConversationNameNavArgs) = metadataFactory.create(args)
        override fun editSelfDeletingMessagesViewModel(args: EditSelfDeletingMessagesNavArgs) =
            selfDeletingFactory.create(args)
        override fun updateChannelAccessViewModel(args: UpdateChannelAccessArgs) = channelAccessFactory.create(args)
        override fun updateAppsAccessViewModel(args: UpdateAppsAccessNavArgs) = appsAccessFactory.create(args)
        override fun editGuestAccessViewModel(args: EditGuestAccessNavArgs) = guestAccessFactory.create(args)
        override fun createPasswordGuestLinkViewModel(args: CreatePasswordGuestLinkNavArgs) = guestLinkFactory.create(args)
    }

    @Provides
    @IntoMap
    @ViewModelKey(CheckAssetRestrictionsViewModel::class)
    fun checkAssetRestrictionsViewModel(viewModel: CheckAssetRestrictionsViewModel): ViewModel = viewModel

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
    @ManualViewModelAssistedFactoryKey(SettingsManualViewModelFactory::class)
    @Suppress("LongParameterList")
    fun settingsManualViewModelFactory(
        verifyEmailFactory: VerifyEmailViewModel.Factory,
        deviceDetailsFactory: DeviceDetailsViewModel.Factory,
        certificateDetailsFactory: E2eiCertificateDetailsViewModel.Factory,
        selfQrCodeFactory: SelfQRCodeViewModel.Factory,
        otherUserProfileFactory: OtherUserProfileScreenViewModel.Factory,
        serviceDetailsFactory: ServiceDetailsViewModelImpl.Factory,
    ): ManualViewModelAssistedFactory = object : SettingsManualViewModelFactory {
        override fun verifyEmailViewModel(arguments: VerifyEmailViewModelArgs) = verifyEmailFactory.create(arguments)
        override fun deviceDetailsViewModel(arguments: DeviceDetailsViewModelArgs) = deviceDetailsFactory.create(arguments)
        override fun e2eiCertificateDetailsViewModel(arguments: E2eiCertificateDetailsViewModelArgs) =
            certificateDetailsFactory.create(arguments)
        override fun selfQRCodeViewModel(arguments: SelfQrCodeViewModelArgs) = selfQrCodeFactory.create(arguments)
        override fun otherUserProfileScreenViewModel(arguments: OtherUserProfileViewModelArgs) =
            otherUserProfileFactory.create(arguments)
        override fun serviceDetailsViewModel(arguments: ServiceDetailsViewModelArgs) = serviceDetailsFactory.create(arguments)
    }
}
