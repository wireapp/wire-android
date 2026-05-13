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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.util.FileHelper
import okio.Path.Companion.toPath
import javax.inject.Inject

class AndroidCellFileExternalActions @Inject constructor(
    private val fileHelper: FileHelper,
) : CellFileExternalActions {

    override fun openLocalFile(
        localPath: String,
        assetName: String?,
        mimeType: String,
        onError: () -> Unit,
    ) {
        fileHelper.openAssetFileWithExternalApp(
            localPath = localPath.toPath(),
            assetName = assetName,
            mimeType = mimeType,
            onError = onError,
        )
    }

    override fun openUrl(
        url: String,
        mimeType: String,
        onError: () -> Unit,
    ) {
        fileHelper.openAssetUrlWithExternalApp(
            url = url,
            mimeType = mimeType,
            onError = onError,
        )
    }

    override fun shareLocalFile(
        localPath: String,
        assetName: String?,
        mimeType: String,
        onError: () -> Unit,
    ) {
        fileHelper.shareFileChooser(
            assetDataPath = localPath.toPath(),
            assetName = assetName,
            mimeType = mimeType,
            onError = onError,
        )
    }
}
