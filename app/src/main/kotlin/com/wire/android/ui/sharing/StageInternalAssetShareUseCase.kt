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

import android.content.Context
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.pathToInternalImportUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okio.Path
import javax.inject.Inject

class StageInternalAssetShareUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stageInternalShare: StageInternalShareUseCase,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(assetDataPath: Path, assetName: String?): String? = withContext(dispatchers.io()) {
        runCatching {
            val assetUri = context.pathToInternalImportUri(assetDataPath, assetName)
            when (val result = stageInternalShare(sharedUris = listOf(assetUri))) {
                is StageInternalShareUseCase.Result.Success -> result.importSessionId
                StageInternalShareUseCase.Result.Failure.InvalidContent,
                StageInternalShareUseCase.Result.Failure.NoSupportedContent -> null
            }
        }.onFailure {
            appLogger.e("Failed to stage internal asset share for $assetDataPath", it)
        }.getOrNull()
    }
}
