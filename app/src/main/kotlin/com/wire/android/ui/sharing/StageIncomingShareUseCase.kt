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
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.util.getProviderAuthority
import com.wire.android.util.parcelableArrayList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StageIncomingShareUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val importSessionStore: ImportSessionStore,
    private val dispatchers: com.wire.android.util.dispatchers.DispatcherProvider,
) {
    suspend operator fun invoke(intent: Intent): Result = withContext(dispatchers.io()) {
        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?.takeIf(String::isNotBlank)
        val sharedUris = extractSharedUris(intent)

        if (sharedText == null && sharedUris.isEmpty()) {
            return@withContext Result.Failure.NoSupportedContent
        }

        val importedAssets = mutableListOf<ImportedMediaAsset>()
        for (sharedUri in sharedUris) {
            if (!sharedUri.isAcceptedIncomingShareUri()) {
                appLogger.w("Rejected incoming share URI: $sharedUri")
                return@withContext Result.Failure.InvalidContent
            }
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

    private fun extractSharedUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE -> intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM).orEmpty()
        else -> emptyList()
    }

    private fun Uri.isAcceptedIncomingShareUri(): Boolean {
        if (scheme != IntentFilterSchemes.CONTENT) {
            return false
        }

        return authority !in setOf(
            context.getProviderAuthority(),
            context.getExportProviderAuthority(),
            context.getImportProviderAuthority()
        )
    }

    sealed class Result {
        data class Success(val importSessionId: String) : Result()

        sealed class Failure : Result() {
            data object InvalidContent : Failure()
            data object NoSupportedContent : Failure()
        }
    }
}

private object IntentFilterSchemes {
    const val CONTENT = "content"
}

fun Context.getImportProviderAuthority() = "$packageName.importprovider"

fun Context.getExportProviderAuthority() = "$packageName.exportprovider"
