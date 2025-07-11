/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.di.accountScoped

import com.wire.android.BuildConfig
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.home.settings.backup.MPBackupSettings
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.backup.BackupScope
import com.wire.kalium.util.DelicateKaliumApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class BackupModule {

    @ViewModelScoped
    @Provides
    fun provideBackupScope(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): BackupScope =
        coreLogic.getSessionScope(currentAccount).backup

    @ViewModelScoped
    @Provides
    fun provideCreateBackupUseCase(backupScope: BackupScope) =
        backupScope.create

    @ViewModelScoped
    @Provides
    fun provideVerifyBackupUseCase(backupScope: BackupScope) =
        backupScope.verify

    @ViewModelScoped
    @Provides
    fun provideRestoreBackupUseCase(backupScope: BackupScope) =
        backupScope.restore

    @Provides
    fun provideMpBackupSettings() = if (BuildConfig.ENABLE_CROSSPLATFORM_BACKUP) {
        MPBackupSettings.Enabled
    } else {
        MPBackupSettings.Disabled
    }

    @OptIn(DelicateKaliumApi::class)
    @ViewModelScoped
    @Provides
    fun provideOnboardingBackupUseCase(backupScope: BackupScope) =
        backupScope.createUnEncryptedCopy
}
