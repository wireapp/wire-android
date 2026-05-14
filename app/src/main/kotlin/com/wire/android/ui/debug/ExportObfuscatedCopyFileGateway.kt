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

package com.wire.android.ui.debug

import androidx.core.net.toUri
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext
import okio.Path

interface ExportObfuscatedCopyFileGateway {
    suspend fun shareCopy(path: Path, assetName: String?)
    suspend fun saveCopy(path: Path, destinationUri: String)
}

class AndroidExportObfuscatedCopyFileGateway @Inject constructor(
    private val fileManager: FileManager,
    private val dispatcher: DispatcherProvider,
) : ExportObfuscatedCopyFileGateway {

    override suspend fun shareCopy(path: Path, assetName: String?) = withContext(dispatcher.io()) {
        fileManager.shareWithExternalApp(path, assetName) {}
    }

    override suspend fun saveCopy(path: Path, destinationUri: String) {
        fileManager.copyToUri(path, destinationUri.toUri(), dispatcher)
    }
}
