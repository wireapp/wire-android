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

import androidx.core.net.toUri
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import kotlinx.coroutines.withContext
import okio.Path

interface BackupFileGateway {
    suspend fun shareBackup(path: Path, assetName: String?)
    suspend fun saveBackup(path: Path, destinationUri: String)
    suspend fun importBackupToTempPath(sourceUri: String): Path
    suspend fun deleteImportedBackup(path: Path)
}

class AndroidBackupFileGateway(
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatcher: DispatcherProvider,
) : BackupFileGateway {

    override suspend fun shareBackup(path: Path, assetName: String?) = withContext(dispatcher.io()) {
        fileManager.shareWithExternalApp(path, assetName) {}
    }

    override suspend fun saveBackup(path: Path, destinationUri: String) {
        fileManager.copyToUri(path, destinationUri.toUri(), dispatcher)
    }

    override suspend fun importBackupToTempPath(sourceUri: String): Path {
        val importedBackupPath = kaliumFileSystem.tempFilePath(
            BackupAndRestoreViewModel.TEMP_IMPORTED_BACKUP_FILE_NAME
        )
        fileManager.copyToPath(sourceUri.toUri(), importedBackupPath, dispatcher)
        return importedBackupPath
    }

    override suspend fun deleteImportedBackup(path: Path) = withContext(dispatcher.io()) {
        if (kaliumFileSystem.exists(path)) {
            kaliumFileSystem.delete(path)
        }
    }
}
