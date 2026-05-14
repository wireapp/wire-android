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
package com.wire.android.ui.sharing

import androidx.core.net.toUri
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext

interface ImportMediaAssetImporter {
    suspend fun importAsset(uri: String): ImportedMediaAsset?
}

class ImportMediaAssetImporterImpl(
    private val handleUriAsset: HandleUriAssetUseCase,
    private val dispatchers: DispatcherProvider,
) : ImportMediaAssetImporter {

    override suspend fun importAsset(uri: String): ImportedMediaAsset? = withContext(dispatchers.io()) {
        when (val result = handleUriAsset.invoke(uri.toUri(), saveToDeviceIfInvalid = false)) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> {
                appLogger.w("$TAG: Failed to import asset message: Asset too large")
                ImportedMediaAsset(result.assetBundle, result.maxLimitInMB)
            }

            HandleUriAssetUseCase.Result.Failure.Unknown -> {
                appLogger.e("$TAG: Failed to import asset message: Unknown error")
                null
            }

            is HandleUriAssetUseCase.Result.Success -> ImportedMediaAsset(result.assetBundle, null)
        }
    }

    private companion object {
        const val TAG = "[ImportMediaAssetImporter]"
    }
}
