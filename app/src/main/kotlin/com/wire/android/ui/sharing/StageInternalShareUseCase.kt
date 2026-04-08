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

import android.net.Uri
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StageInternalShareUseCase @Inject constructor(
    private val handleUriAsset: HandleUriAssetUseCase,
    private val importSessionStore: ImportSessionStore,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(sharedUris: List<Uri>, sharedText: String? = null): Result = withContext(dispatchers.io()) {
        if (sharedText.isNullOrBlank() && sharedUris.isEmpty()) {
            return@withContext Result.Failure.NoSupportedContent
        }

        val importedAssets = mutableListOf<ImportedMediaAsset>()
        for (sharedUri in sharedUris) {
            when (val result = handleUriAsset.invoke(sharedUri, saveToDeviceIfInvalid = false)) {
                is HandleUriAssetUseCase.Result.Success -> importedAssets += ImportedMediaAsset(result.assetBundle, null)
                is HandleUriAssetUseCase.Result.Failure.AssetTooLarge ->
                    importedAssets += ImportedMediaAsset(result.assetBundle, result.maxLimitInMB)

                HandleUriAssetUseCase.Result.Failure.Unknown -> return@withContext Result.Failure.InvalidContent
            }
        }

        val sessionId = importSessionStore.store(sharedText, importedAssets)
        Result.Success(sessionId)
    }

    sealed class Result {
        data class Success(val importSessionId: String) : Result()

        sealed class Failure : Result() {
            data object InvalidContent : Failure()
            data object NoSupportedContent : Failure()
        }
    }
}
