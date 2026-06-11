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
package com.wire.android.ui.home.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModel
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModel
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModel
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
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
import com.wire.android.ui.home.settings.privacy.PrivacySettingsViewModel
import com.wire.android.ui.settings.devices.DeviceDetailsViewModel
import com.wire.android.ui.settings.devices.SelfDevicesViewModel
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsViewModel
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModel
import com.wire.android.ui.userprofile.qr.SelfQRCodeViewModel
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel
import com.wire.android.ui.userprofile.service.ServiceDetailsViewModelImpl
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberUseCase
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.debug.BreakSessionUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase as GetE2EIMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import com.wire.android.di.ApplicationContext
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider

@Suppress("LongParameterList", "TooManyFunctions")
class SettingsViewModelFactory @Inject constructor(
    private val settingsViewModel: Provider<SettingsViewModel>,
    private val myAccountViewModel: Provider<MyAccountViewModel>,
    private val deleteAccountViewModel: Provider<DeleteAccountViewModel>,
    private val changeDisplayNameViewModel: Provider<ChangeDisplayNameViewModel>,
    private val changeUserColorViewModel: Provider<ChangeUserColorViewModel>,
    private val changeEmailViewModel: Provider<ChangeEmailViewModel>,
    private val changeHandleViewModel: Provider<ChangeHandleViewModel>,
    private val customizationViewModel: Provider<CustomizationViewModel>,
    private val networkSettingsViewModel: Provider<NetworkSettingsViewModel>,
    private val privacySettingsViewModel: Provider<PrivacySettingsViewModel>,
    private val backupAndRestoreViewModel: Provider<BackupAndRestoreViewModel>,
    private val setLockScreenViewModel: Provider<SetLockScreenViewModel>,
    private val forgotLockScreenViewModel: Provider<ForgotLockScreenViewModel>,
    private val appUnlockWithBiometricsViewModel: Provider<AppUnlockWithBiometricsViewModel>,
    private val enterLockScreenViewModel: Provider<EnterLockScreenViewModel>,
    private val selfDevicesViewModel: Provider<SelfDevicesViewModel>,
    private val avatarPickerViewModel: Provider<AvatarPickerViewModel>,
    private val selfUserProfileViewModel: Provider<SelfUserProfileViewModel>,
    private val teamMigrationViewModel: Provider<TeamMigrationViewModel>,
    private val updateEmail: Provider<UpdateEmailUseCase>,
    private val getSelfUser: Provider<GetSelfUserUseCase>,
    @CurrentAccount private val currentUserId: Provider<UserId>,
    private val deleteClient: Provider<DeleteClientUseCase>,
    private val observeClientDetails: Provider<ObserveClientDetailsUseCase>,
    private val isPasswordRequired: Provider<IsPasswordRequiredUseCase>,
    private val fingerprintUseCase: Provider<ClientFingerprintUseCase>,
    private val updateClientVerificationStatus: Provider<UpdateClientVerificationStatusUseCase>,
    private val observeUserInfo: Provider<ObserveUserInfoUseCase>,
    private val mlsClientIdentity: Provider<GetE2EIMLSClientIdentityUseCase>,
    private val breakSession: Provider<BreakSessionUseCase>,
    private val isE2EIEnabledUseCase: Provider<IsE2EIEnabledUseCase>,
    @ApplicationContext private val context: Provider<Context>,
    private val selfServerLinks: Provider<SelfServerConfigUseCase>,
    private val kaliumFileSystem: Provider<KaliumFileSystem>,
    private val dispatchers: Provider<DispatcherProvider>,
    private val analyticsManager: Provider<AnonymousAnalyticsManager>,
    private val userTypeMapper: Provider<UserTypeMapper>,
    private val observeConversationRoleForUser: Provider<ObserveConversationRoleForUserUseCase>,
    private val removeMemberFromConversation: Provider<RemoveMemberFromConversationUseCase>,
    private val updateMemberRole: Provider<UpdateConversationMemberRoleUseCase>,
    private val observeClientList: Provider<ObserveClientsByUserIdUseCase>,
    private val fetchUsersClients: Provider<FetchUsersClientsFromRemoteUseCase>,
    private val getUserE2eiCertificateStatus: Provider<IsOtherUserE2EIVerifiedUseCase>,
    private val isOneToOneConversationCreated: Provider<IsOneToOneConversationCreatedUseCase>,
    private val getServiceById: Provider<GetServiceByIdUseCase>,
    private val getAppById: Provider<GetAppByIdUseCase>,
    private val observeConversationDetails: Provider<ObserveConversationDetailsUseCase>,
    private val observeIsServiceMember: Provider<ObserveIsServiceMemberUseCase>,
    private val observeIsAppMember: Provider<ObserveIsAppMemberUseCase>,
    private val observeIsAppsAllowedForUsage: Provider<ObserveIsAppsAllowedForUsageUseCase>,
    private val addServiceToConversation: Provider<AddServiceToConversationUseCase>,
    private val addMemberToConversation: Provider<AddMemberToConversationUseCase>,
    private val getOrCreateOneToOneConversation: Provider<GetOrCreateOneToOneConversationUseCase>,
) {
    fun settingsViewModel(): SettingsViewModel = settingsViewModel.invoke()
    fun myAccountViewModel(): MyAccountViewModel = myAccountViewModel.invoke()
    fun deleteAccountViewModel(): DeleteAccountViewModel = deleteAccountViewModel.invoke()
    fun changeDisplayNameViewModel(): ChangeDisplayNameViewModel = changeDisplayNameViewModel.invoke()
    fun changeUserColorViewModel(): ChangeUserColorViewModel = changeUserColorViewModel.invoke()
    fun changeEmailViewModel(): ChangeEmailViewModel = changeEmailViewModel.invoke()
    fun changeHandleViewModel(): ChangeHandleViewModel = changeHandleViewModel.invoke()
    fun customizationViewModel(): CustomizationViewModel = customizationViewModel.invoke()
    fun networkSettingsViewModel(): NetworkSettingsViewModel = networkSettingsViewModel.invoke()
    fun privacySettingsViewModel(): PrivacySettingsViewModel = privacySettingsViewModel.invoke()
    fun backupAndRestoreViewModel(): BackupAndRestoreViewModel = backupAndRestoreViewModel.invoke()
    fun setLockScreenViewModel(): SetLockScreenViewModel = setLockScreenViewModel.invoke()
    fun forgotLockScreenViewModel(): ForgotLockScreenViewModel = forgotLockScreenViewModel.invoke()
    fun appUnlockWithBiometricsViewModel(): AppUnlockWithBiometricsViewModel = appUnlockWithBiometricsViewModel.invoke()
    fun enterLockScreenViewModel(): EnterLockScreenViewModel = enterLockScreenViewModel.invoke()
    fun selfDevicesViewModel(): SelfDevicesViewModel = selfDevicesViewModel.invoke()
    fun avatarPickerViewModel(): AvatarPickerViewModel = avatarPickerViewModel.invoke()
    fun selfUserProfileViewModel(): SelfUserProfileViewModel = selfUserProfileViewModel.invoke()
    fun teamMigrationViewModel(): TeamMigrationViewModel = teamMigrationViewModel.invoke()

    fun verifyEmailViewModel(savedStateHandle: SavedStateHandle): VerifyEmailViewModel = VerifyEmailViewModel(
        updateEmail = updateEmail(),
        savedStateHandle = savedStateHandle,
    )

    fun e2eiCertificateDetailsViewModel(savedStateHandle: SavedStateHandle): E2eiCertificateDetailsViewModel =
        E2eiCertificateDetailsViewModel(
            savedStateHandle = savedStateHandle,
            getSelfUser = getSelfUser(),
        )

    fun deviceDetailsViewModel(savedStateHandle: SavedStateHandle): DeviceDetailsViewModel = DeviceDetailsViewModel(
        savedStateHandle = savedStateHandle,
        currentUserId = currentUserId(),
        deleteClient = deleteClient(),
        observeClientDetails = observeClientDetails(),
        isPasswordRequired = isPasswordRequired(),
        fingerprintUseCase = fingerprintUseCase(),
        updateClientVerificationStatus = updateClientVerificationStatus(),
        observeUserInfo = observeUserInfo(),
        mlsClientIdentity = mlsClientIdentity(),
        breakSession = breakSession(),
        isE2EIEnabledUseCase = isE2EIEnabledUseCase(),
    )

    fun selfQRCodeViewModel(savedStateHandle: SavedStateHandle): SelfQRCodeViewModel = SelfQRCodeViewModel(
        savedStateHandle = savedStateHandle,
        context = context(),
        selfUserId = currentUserId(),
        selfServerLinks = selfServerLinks(),
        kaliumFileSystem = kaliumFileSystem(),
        dispatchers = dispatchers(),
        analyticsManager = analyticsManager(),
    )

    fun otherUserProfileScreenViewModel(savedStateHandle: SavedStateHandle): OtherUserProfileScreenViewModel =
        OtherUserProfileScreenViewModel(
            dispatchers = dispatchers(),
            observeUserInfo = observeUserInfo(),
            userTypeMapper = userTypeMapper(),
            observeConversationRoleForUser = observeConversationRoleForUser(),
            removeMemberFromConversation = removeMemberFromConversation(),
            updateMemberRole = updateMemberRole(),
            observeClientList = observeClientList(),
            fetchUsersClients = fetchUsersClients(),
            getUserE2eiCertificateStatus = getUserE2eiCertificateStatus(),
            isOneToOneConversationCreated = isOneToOneConversationCreated(),
            mlsClientIdentity = mlsClientIdentity(),
            isE2EIEnabled = isE2EIEnabledUseCase(),
            savedStateHandle = savedStateHandle,
        )

    fun serviceDetailsViewModel(savedStateHandle: SavedStateHandle): ServiceDetailsViewModelImpl = ServiceDetailsViewModelImpl(
        dispatchers = dispatchers(),
        selfUserId = currentUserId(),
        getServiceById = getServiceById(),
        getAppById = getAppById(),
        observeConversationDetails = observeConversationDetails(),
        observeIsServiceMember = observeIsServiceMember(),
        observeIsAppMember = observeIsAppMember(),
        observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage(),
        observeConversationRoleForUser = observeConversationRoleForUser(),
        removeMemberFromConversation = removeMemberFromConversation(),
        addServiceToConversation = addServiceToConversation(),
        addMemberToConversation = addMemberToConversation(),
        isOneToOneConversationCreated = isOneToOneConversationCreated(),
        getOrCreateOneToOneConversation = getOrCreateOneToOneConversation(),
        savedStateHandle = savedStateHandle,
    )
}
