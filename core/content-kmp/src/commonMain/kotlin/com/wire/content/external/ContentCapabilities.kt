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

import com.wire.content.asset.PreparedAsset
import okio.Path
import okio.Sink

/** A file already owned by the app and safe to expose through a platform capability. */
data class LocalContent(
    val path: Path,
    val displayName: String,
    val mimeType: String? = null,
    val size: Long? = null,
)

data class RemoteContent(
    val location: String,
    val mimeType: String,
)

enum class CaptureKind { IMAGE, VIDEO }

/** Reads external content into app-private storage. URI validation belongs in the implementation. */
interface ExternalContentReader {
    suspend fun prepare(
        request: ExternalContentImportRequest,
        destination: Path,
        assetKey: String,
    ): PlatformResult<PreparedAsset>

    suspend fun copyToPrivateStorage(
        source: ExternalContentReference,
        destination: Path,
    ): PlatformResult<Long>
}

/** Writes app-owned content to a user-selected or public platform destination. */
interface FileExporter {
    suspend fun exportToDownloads(content: LocalContent): PlatformResult<String?>
    suspend fun exportToMedia(content: LocalContent): PlatformResult<String?>
    suspend fun write(content: LocalContent, destination: ExternalContentReference): PlatformResult<Unit>
    suspend fun openDownloadSink(displayName: String): PlatformResult<Sink>
}

/** Opens or shares content through native platform UI. */
interface ExternalFileLauncher {
    fun open(content: LocalContent): PlatformResult<Unit>
    fun share(content: LocalContent): PlatformResult<Unit>
    fun openRemote(content: RemoteContent): PlatformResult<Unit>
    fun shareText(text: String): PlatformResult<Unit>
}

/** Creates a platform-owned destination that camera applications may write into. */
interface CaptureTargetProvider {
    suspend fun createTarget(kind: CaptureKind, cacheRoot: Path): PlatformResult<ExternalContentReference>
}

fun interface AssetSizeLimitProvider {
    suspend fun maximumSizeBytes(isImage: Boolean): Long
}

fun interface ContentKeyGenerator {
    fun nextKey(): String
}

fun interface AssetImporter {
    suspend operator fun invoke(request: ExternalContentImportRequest): ExternalContentImportResult
}
