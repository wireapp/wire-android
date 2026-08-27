/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.content.external

import com.wire.content.asset.AssetFileNamePolicy
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import kotlinx.coroutines.CancellationException

/** Platform-neutral import policy and orchestration. */
class AssetImportCoordinator(
    private val reader: ExternalContentReader,
    private val exporter: FileExporter,
    private val fileSystem: KaliumFileSystem,
    private val sizeLimitProvider: AssetSizeLimitProvider,
    private val keyGenerator: ContentKeyGenerator,
) : AssetImporter {
    override suspend operator fun invoke(request: ExternalContentImportRequest): ExternalContentImportResult {
        val key = keyGenerator.nextKey()
        val destination = fileSystem.tempFilePath(key)
        return try {
            when (val prepared = reader.prepare(request, destination, key)) {
                is PlatformResult.Success -> mapPreparedAsset(request, prepared.value)
                PlatformResult.Cancelled -> cleanup(destination, ExternalContentImportResult.Cancelled)
                PlatformResult.Unsupported -> cleanup(destination, ExternalContentImportResult.Unsupported)
                is PlatformResult.Failure -> cleanup(destination, ExternalContentImportResult.Failure)
            }
        } catch (cancelled: CancellationException) {
            fileSystem.delete(destination)
            throw cancelled
        } catch (_: Exception) {
            cleanup(destination, ExternalContentImportResult.Failure)
        }
    }

    private suspend fun mapPreparedAsset(
        request: ExternalContentImportRequest,
        unvalidatedAsset: com.wire.content.asset.PreparedAsset,
    ): ExternalContentImportResult {
        val asset = unvalidatedAsset
        val maximumSize = sizeLimitProvider.maximumSizeBytes(asset.assetType == AttachmentType.IMAGE)
        if (asset.dataSize <= maximumSize) return ExternalContentImportResult.Success(asset)

        if (request.saveToDeviceIfInvalid) {
            exporter.exportToMedia(
                LocalContent(
                    path = asset.dataPath,
                    displayName = AssetFileNamePolicy.sanitize(asset.fileName),
                    mimeType = asset.mimeType,
                    size = asset.dataSize,
                )
            )
        }
        return ExternalContentImportResult.TooLarge(asset, maximumSize)
    }

    private fun cleanup(
        path: okio.Path,
        result: ExternalContentImportResult,
    ): ExternalContentImportResult {
        fileSystem.delete(path)
        return result
    }
}
