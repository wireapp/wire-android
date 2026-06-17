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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.sessionKeyedMetroViewModel
import com.wire.android.ui.home.appLock.forgot.ForgotLockScreenViewModel
import com.wire.android.ui.home.appLock.set.SetLockScreenViewModel
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsViewModel
import com.wire.android.ui.home.appLock.unlock.EnterLockScreenViewModel
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
import com.wire.android.ui.userprofile.self.status.SelfUserStatusViewModel
import com.wire.android.ui.userprofile.service.ServiceDetailsViewModel
import com.wire.android.ui.userprofile.service.ServiceDetailsViewModelImpl
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel

@Composable
inline fun <reified VM> settingsViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
): VM where VM : ViewModel =
    sessionKeyedMetroViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    )

@Composable
inline fun <reified VM> settingsSavedStateViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
): VM where VM : ViewModel =
    settingsViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    )

@Composable
fun settingsScreenViewModel(): SettingsViewModel = settingsViewModel()

@Composable
fun myAccountViewModel(): MyAccountViewModel = settingsViewModel()

@Composable
fun deleteAccountViewModel(): DeleteAccountViewModel =
    settingsViewModel()

@Composable
fun changeDisplayNameViewModel(): ChangeDisplayNameViewModel =
    settingsViewModel()

@Composable
fun changeUserColorViewModel(): ChangeUserColorViewModel =
    settingsViewModel()

@Composable
fun changeEmailViewModel(): ChangeEmailViewModel = settingsViewModel()

@Composable
fun verifyEmailViewModel(): VerifyEmailViewModel =
    settingsSavedStateViewModel()

@Composable
fun changeHandleViewModel(): ChangeHandleViewModel = settingsViewModel()

@Composable
fun customizationViewModel(): CustomizationViewModel =
    settingsViewModel()

@Composable
fun networkSettingsViewModel(): NetworkSettingsViewModel =
    settingsViewModel()

@Composable
fun privacySettingsViewModel(): PrivacySettingsViewModel =
    settingsViewModel()

@Composable
fun backupAndRestoreViewModel(): BackupAndRestoreViewModel =
    settingsViewModel()

@Composable
fun setLockScreenViewModel(): SetLockScreenViewModel =
    settingsViewModel()

@Composable
fun forgotLockScreenViewModel(): ForgotLockScreenViewModel =
    settingsViewModel()

@Composable
fun appUnlockWithBiometricsViewModel(): AppUnlockWithBiometricsViewModel =
    settingsViewModel()

@Composable
fun enterLockScreenViewModel(): EnterLockScreenViewModel =
    settingsViewModel()

@Composable
fun selfDevicesViewModel(): SelfDevicesViewModel = settingsViewModel()

@Composable
fun deviceDetailsViewModel(): DeviceDetailsViewModel =
    settingsSavedStateViewModel()

@Composable
fun e2eiCertificateDetailsViewModel(): E2eiCertificateDetailsViewModel =
    settingsSavedStateViewModel()

@Composable
fun avatarPickerViewModel(): AvatarPickerViewModel =
    settingsViewModel()

@Composable
fun selfUserProfileViewModel(): SelfUserProfileViewModel =
    settingsViewModel()

@Composable
fun selfUserStatusViewModel(): SelfUserStatusViewModel =
    settingsViewModel()

@Composable
fun selfQRCodeViewModel(): SelfQRCodeViewModel =
    settingsSavedStateViewModel()

@Composable
fun teamMigrationViewModel(
    viewModelStoreOwner: ViewModelStoreOwner,
): TeamMigrationViewModel = settingsViewModel(viewModelStoreOwner = viewModelStoreOwner)

@Composable
fun otherUserProfileScreenViewModel(): OtherUserProfileScreenViewModel =
    settingsSavedStateViewModel()

@Composable
fun serviceDetailsViewModel(): ServiceDetailsViewModel =
    settingsSavedStateViewModel<ServiceDetailsViewModelImpl>()
