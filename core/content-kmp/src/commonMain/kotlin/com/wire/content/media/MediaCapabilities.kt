/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.content.media

import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import com.wire.kalium.logic.data.message.AssetContent
import okio.Path

sealed interface ContentImageSource {
    data class Local(val path: Path) : ContentImageSource
    data class External(val reference: ExternalContentReference) : ContentImageSource
}

enum class ImageResizeProfile {
    ORIGINAL,
    ATTACHMENT,
    AVATAR,
}

data class ImageProcessingRequest(
    val source: ExternalContentReference,
    val destination: Path,
    val resizeProfile: ImageResizeProfile,
    val removeMetadata: Boolean,
)

data class ProcessedImage(
    val path: Path,
    val sizeBytes: Long,
)

data class EncodedImage(
    val bytes: ByteArray,
    val mimeType: String,
)

data class EncodedImageExportRequest(
    val image: EncodedImage,
    val displayName: String,
    val fallbackPath: Path,
    val destination: ExternalContentReference? = null,
)

fun interface MediaMetadataReader {
    suspend fun read(path: Path, mimeType: String): PlatformResult<AssetContent.AssetMetadata?>
}

fun interface ImageProcessor {
    suspend fun process(request: ImageProcessingRequest): PlatformResult<ProcessedImage>
}

fun interface ImageTargetProvider {
    fun createTarget(path: Path): PlatformResult<ExternalContentReference>
}

fun interface EncodedImageExporter {
    suspend fun export(request: EncodedImageExportRequest): PlatformResult<ExternalContentReference>
}

object UnsupportedMediaMetadataReader : MediaMetadataReader {
    override suspend fun read(path: Path, mimeType: String): PlatformResult<AssetContent.AssetMetadata?> =
        PlatformResult.Unsupported
}

object UnsupportedImageProcessor : ImageProcessor {
    override suspend fun process(request: ImageProcessingRequest): PlatformResult<ProcessedImage> = PlatformResult.Unsupported
}

object UnsupportedImageTargetProvider : ImageTargetProvider {
    override fun createTarget(path: Path): PlatformResult<ExternalContentReference> = PlatformResult.Unsupported
}

object UnsupportedEncodedImageExporter : EncodedImageExporter {
    override suspend fun export(request: EncodedImageExportRequest): PlatformResult<ExternalContentReference> =
        PlatformResult.Unsupported
}
