/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.platform.content

import android.content.Context
import android.net.Uri
import com.wire.android.appLogger
import com.wire.android.di.ApplicationContext
import com.wire.android.util.ImageUtil
import com.wire.android.util.MediaMetadata
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getTempWritableAttachmentUri
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.content.asset.AssetFileNamePolicy
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import com.wire.content.external.asAndroidUri
import com.wire.content.external.asExternalContentReference
import com.wire.content.media.EncodedImageExportRequest
import com.wire.content.media.EncodedImageExporter
import com.wire.content.media.ImageProcessingRequest
import com.wire.content.media.ImageProcessor
import com.wire.content.media.ImageResizeProfile
import com.wire.content.media.ImageTargetProvider
import com.wire.content.media.MediaMetadataReader
import com.wire.content.media.ProcessedImage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<MediaMetadataReader>())
class AndroidMediaMetadataReader(
    private val dispatchers: DispatcherProvider,
) : MediaMetadataReader {
    override suspend fun read(
        path: okio.Path,
        mimeType: String,
    ) = withContext(dispatchers.io()) {
        try {
            PlatformResult.Success(MediaMetadata.getMediaMetadata(path, mimeType))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            appLogger.e("Media metadata could not be read", error)
            PlatformResult.Failure(error.message)
        } catch (error: IllegalArgumentException) {
            appLogger.e("Media metadata could not be read", error)
            PlatformResult.Failure(error.message)
        } catch (error: IllegalStateException) {
            appLogger.e("Media metadata could not be read", error)
            PlatformResult.Failure(error.message)
        } catch (error: SecurityException) {
            appLogger.e("Media metadata could not be read", error)
            PlatformResult.Failure(error.message)
        }
    }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ImageProcessor>())
class AndroidImageProcessor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : ImageProcessor {
    override suspend fun process(request: ImageProcessingRequest): PlatformResult<ProcessedImage> =
        withContext(dispatchers.io()) {
            try {
                val uri = request.source.asAndroidUri().also(Uri::requireExternalContentUri)
                val size = when (request.resizeProfile) {
                    ImageResizeProfile.ORIGINAL -> copyOriginal(uri, request.destination)
                    ImageResizeProfile.ATTACHMENT -> uri.resampleImageAndCopyToTempPath(
                        context = context,
                        tempCachePath = request.destination,
                        sizeClass = ImageUtil.ImageSizeClass.Medium,
                        shouldRemoveMetadata = request.removeMetadata,
                        dispatcher = dispatchers,
                    )
                    ImageResizeProfile.AVATAR -> uri.resampleImageAndCopyToTempPath(
                        context = context,
                        tempCachePath = request.destination,
                        sizeClass = ImageUtil.ImageSizeClass.Small,
                        shouldRemoveMetadata = request.removeMetadata,
                        dispatcher = dispatchers,
                    )
                }
                PlatformResult.Success(ProcessedImage(request.destination, size))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                appLogger.e("Image processing failed", error)
                PlatformResult.Failure(error.message)
            } catch (error: SecurityException) {
                appLogger.e("Image permission was denied", error)
                PlatformResult.Failure("permission_denied")
            } catch (error: IllegalArgumentException) {
                PlatformResult.Unsupported
            }
        }

    private fun copyOriginal(source: Uri, destination: okio.Path): Long =
        context.contentResolver.openInputStream(source).use { input ->
            destination.toFile().outputStream().use { output ->
                input?.copyTo(output) ?: throw IOException("Content stream is unavailable")
            }
        }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ImageTargetProvider>())
class AndroidImageTargetProvider(
    @ApplicationContext private val context: Context,
) : ImageTargetProvider {
    override fun createTarget(path: okio.Path): PlatformResult<ExternalContentReference> = try {
        PlatformResult.Success(getTempWritableAttachmentUri(context, path).asExternalContentReference())
    } catch (error: IllegalArgumentException) {
        PlatformResult.Failure(error.message)
    } catch (error: SecurityException) {
        PlatformResult.Failure("permission_denied")
    }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<EncodedImageExporter>())
class AndroidEncodedImageExporter(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : EncodedImageExporter {
    override suspend fun export(request: EncodedImageExportRequest): PlatformResult<ExternalContentReference> =
        withContext(dispatchers.io()) {
            try {
                val safeFallbackPath = request.fallbackPath.parent
                    ?.let { parent -> parent / AssetFileNamePolicy.sanitize(request.displayName) }
                    ?: request.fallbackPath
                val destination = request.destination?.asAndroidUri()?.also(Uri::requireExternalContentUri)
                    ?: getTempWritableAttachmentUri(context, safeFallbackPath)
                val descriptor = context.contentResolver.openFileDescriptor(destination, "rwt")
                    ?: return@withContext PlatformResult.Failure("destination_unavailable")
                descriptor.use {
                    FileOutputStream(it.fileDescriptor).use { output ->
                        output.write(request.image.bytes)
                        output.flush()
                    }
                }
                PlatformResult.Success(destination.asExternalContentReference())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                appLogger.e("Encoded image could not be exported", error)
                PlatformResult.Failure(error.message)
            } catch (error: SecurityException) {
                appLogger.e("Image export permission was denied", error)
                PlatformResult.Failure("permission_denied")
            } catch (error: IllegalArgumentException) {
                PlatformResult.Unsupported
            }
        }
}
