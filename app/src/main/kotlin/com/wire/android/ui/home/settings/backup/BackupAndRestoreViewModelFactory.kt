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
package com.wire.android.ui.home.settings.backup

import com.wire.android.datastore.UserDataStore
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.backup.CreateBackupUseCase
import com.wire.kalium.logic.feature.backup.CreateMPBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreBackupUseCase
import com.wire.kalium.logic.feature.backup.RestoreMPBackupUseCase
import com.wire.kalium.logic.feature.backup.VerifyBackupUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class BackupAndRestoreViewModelFactory(
    private val importBackup: RestoreBackupUseCase,
    private val importMpBackup: RestoreMPBackupUseCase,
    private val createBackupFile: CreateBackupUseCase,
    private val createMpBackupFile: CreateMPBackupUseCase,
    private val verifyBackup: VerifyBackupUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val backupFileGateway: BackupFileGateway,
    private val userDataStore: UserDataStore,
    private val dispatcher: DispatcherProvider,
    private val mpBackupSettings: MPBackupSettings,
) {
    fun create(): BackupAndRestoreViewModel = BackupAndRestoreViewModel(
        importBackup = importBackup,
        importMpBackup = importMpBackup,
        createBackupFile = createBackupFile,
        createMpBackupFile = createMpBackupFile,
        verifyBackup = verifyBackup,
        validatePassword = validatePassword,
        backupFileGateway = backupFileGateway,
        userDataStore = userDataStore,
        dispatcher = dispatcher,
        mpBackupSettings = mpBackupSettings,
    )
}
