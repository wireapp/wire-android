/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.usecase

import android.net.Uri
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class HandleUriAssetUseCase @Inject constructor(
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatchers: DispatcherProvider
) {

    suspend fun invoke(
        uri: Uri,
        saveToDeviceIfInvalid: Boolean = false,
        specifiedMimeType: String? = null, // specify a particular mimetype, otherwise it will be taken from the uri / file extension
    ): Result = withContext(dispatchers.io()) {
        if (!isValidUriSchema(uri)) {
            return@withContext Result.Failure.Unknown
        }

        val tempAssetPath = kaliumFileSystem.tempFilePath(UUID.randomUUID().toString())
        val assetBundle = fileManager.getAssetBundleFromUri(
            attachmentUri = uri,
            assetDestinationPath = tempAssetPath,
            specifiedMimeType = specifiedMimeType,
        )
        if (assetBundle != null) {
            // The max limit for sending assets changes between user and asset types.
            // Check [GetAssetSizeLimitUseCase] class for more detailed information about the real limits.
            val maxSizeLimitInBytes = getAssetSizeLimit(isImage = assetBundle.assetType == AttachmentType.IMAGE)

            if (assetBundle.dataSize <= maxSizeLimitInBytes) {
                return@withContext Result.Success(assetBundle)
            } else {
                if (saveToDeviceIfInvalid) {
                    with(assetBundle) {
                        fileManager.saveToExternalMediaStorage(
                            fileName,
                            dataPath,
                            dataSize,
                            mimeType,
                            dispatchers
                        )
                    }
                }
                return@withContext Result.Failure.AssetTooLarge(assetBundle, maxSizeLimitInBytes.div(sizeOf1MB).toInt())
            }
        } else {
            return@withContext Result.Failure.Unknown
        }
    }

    /**
     * Handles the correctness of the supported schema of the URI.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun isValidUriSchema(uri: Uri): Boolean {
        return try {
            fileManager.checkValidSchema(uri)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val sizeOf1MB = 1024 * 1024
    }

    sealed class Result {
        data class Success(val assetBundle: AssetBundle) : Result()
        sealed class Failure : Result() {
            data class AssetTooLarge(val assetBundle: AssetBundle, val maxLimitInMB: Int) : Failure()
            data object Unknown : Failure()
        }
    }
}
