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

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wire.android.appLogger
import com.wire.android.di.ApplicationContext
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.copyFile
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getFileName
import com.wire.android.util.getMimeType
import com.wire.android.util.getTempWritableAttachmentUri
import com.wire.android.util.openAssetFileWithExternalApp
import com.wire.android.util.openAssetUrlWithExternalApp
import com.wire.android.util.orDefault
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.android.util.saveFileDataToMediaFolder
import com.wire.android.util.saveFileToDownloadsFolder
import com.wire.android.util.shareAssetFileWithExternalApp
import com.wire.android.util.startShareIntentWithTrustedWireTarget
import com.wire.content.asset.AssetFileNamePolicy
import com.wire.content.asset.PreparedAsset
import com.wire.content.external.CaptureKind
import com.wire.content.external.CaptureTargetProvider
import com.wire.content.external.ContentKeyGenerator
import com.wire.content.external.ExternalContentImportRequest
import com.wire.content.external.ExternalContentReader
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.ExternalFileLauncher
import com.wire.content.external.FileExporter
import com.wire.content.external.LocalContent
import com.wire.content.external.PlatformResult
import com.wire.content.external.RemoteContent
import com.wire.kalium.logic.data.asset.AttachmentType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.Path
import okio.Sink
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ExternalContentReader>())
class AndroidExternalContentReader(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : ExternalContentReader {
    override suspend fun prepare(
        request: ExternalContentImportRequest,
        destination: Path,
        assetKey: String,
    ): PlatformResult<PreparedAsset> = withContext(dispatchers.io()) {
        try {
            val uri = request.reference.asAndroidUri().also(Uri::requireExternalContentUri)
            val fileName = context.getFileName(uri) ?: throw IOException("The selected asset has an invalid name")
            val mimeType = request.mimeType ?: uri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE)
            val attachmentType = AttachmentType.fromMimeTypeString(mimeType)
            val size = if (attachmentType == AttachmentType.IMAGE) {
                uri.resampleImageAndCopyToTempPath(context, destination)
            } else {
                copy(uri, destination)
            }
            PlatformResult.Success(
                PreparedAsset(
                    key = assetKey,
                    mimeType = mimeType,
                    dataPath = destination,
                    dataSize = size,
                    fileName = fileName,
                    assetType = attachmentType,
                    audioWavesMask = request.audioWavesMask,
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: SecurityException) {
            appLogger.e("External content permission was denied", error)
            PlatformResult.Failure("permission_denied")
        } catch (error: IOException) {
            appLogger.e("External content could not be read", error)
            PlatformResult.Failure("io_error")
        } catch (error: IllegalArgumentException) {
            appLogger.e("External content reference was rejected", error)
            PlatformResult.Unsupported
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun copyToPrivateStorage(
        source: ExternalContentReference,
        destination: Path,
    ): PlatformResult<Long> = withContext(dispatchers.io()) {
        try {
            val uri = source.asAndroidUri().also(Uri::requireExternalContentUri)
            PlatformResult.Success(copy(uri, destination))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            appLogger.e("External content could not be copied", error)
            PlatformResult.Failure(error.message)
        }
    }

    private fun copy(uri: Uri, destination: Path): Long {
        val file = destination.toFile().apply { setWritable(true) }
        return context.contentResolver.openInputStream(uri).use { input ->
            file.outputStream().use { output -> input?.copyTo(output) ?: throw IOException("Content stream is unavailable") }
        }
    }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<FileExporter>())
class AndroidFileExporter(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : FileExporter {
    override suspend fun exportToDownloads(content: LocalContent): PlatformResult<String?> = withContext(dispatchers.io()) {
        runExport {
            saveFileToDownloadsFolder(content.displayName, content.path, content.resolvedSize(), context)
                ?.let(context::getFileName)
        }
    }

    override suspend fun exportToMedia(content: LocalContent): PlatformResult<String?> = withContext(dispatchers.io()) {
        runExport {
            saveFileDataToMediaFolder(
                content.displayName,
                content.path,
                content.resolvedSize(),
                content.mimeType.orDefault(DEFAULT_FILE_MIME_TYPE),
                context,
            )?.let(context::getFileName)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun write(
        content: LocalContent,
        destination: ExternalContentReference,
    ): PlatformResult<Unit> = withContext(dispatchers.io()) {
        try {
            val uri = destination.asAndroidUri().also(Uri::requireExternalContentUri)
            context.contentResolver.copyFile(uri, content.path)
            PlatformResult.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            appLogger.e("Content could not be exported", error)
            PlatformResult.Failure(error.message)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun openDownloadSink(displayName: String): PlatformResult<Sink> = withContext(dispatchers.io()) {
        try {
            val safeDisplayName = AssetFileNamePolicy.sanitize(displayName)
            val output = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeDisplayName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?.let(context.contentResolver::openOutputStream)
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                FileOutputStream(File(downloads, safeDisplayName))
            }
            output?.sink()?.let { PlatformResult.Success(it) } ?: PlatformResult.Failure("destination_unavailable")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            PlatformResult.Failure(error.message)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> runExport(block: () -> T): PlatformResult<T> = try {
        PlatformResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        appLogger.e("Content could not be exported", error)
        PlatformResult.Failure(error.message)
    }

    private fun LocalContent.resolvedSize(): Long = size ?: path.toFile().length()
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ExternalFileLauncher>())
class AndroidExternalFileLauncher(
    @ApplicationContext private val context: Context,
) : ExternalFileLauncher {
    override fun open(content: LocalContent): PlatformResult<Unit> = launch {
        openAssetFileWithExternalApp(content.path, context, content.displayName, content.mimeType, it)
    }

    override fun share(content: LocalContent): PlatformResult<Unit> = launch {
        shareAssetFileWithExternalApp(content.path, context, content.displayName, it)
    }

    override fun openRemote(content: RemoteContent): PlatformResult<Unit> = launch {
        openAssetUrlWithExternalApp(content.location, content.mimeType, context, it)
    }

    override fun shareText(text: String): PlatformResult<Unit> = launch { onError ->
        try {
            context.startShareIntentWithTrustedWireTarget(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            )
        } catch (_: Exception) {
            onError()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun launch(block: (() -> Unit) -> Unit): PlatformResult<Unit> {
        return try {
            var failed = false
            block { failed = true }
            if (failed) PlatformResult.Failure() else PlatformResult.Success(Unit)
        } catch (error: Exception) {
            appLogger.e("External content action failed", error)
            PlatformResult.Failure(error.message)
        }
    }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<CaptureTargetProvider>())
class AndroidCaptureTargetProvider(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : CaptureTargetProvider {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun createTarget(
        kind: CaptureKind,
        cacheRoot: Path,
    ): PlatformResult<ExternalContentReference> = withContext(dispatchers.io()) {
        try {
            val name = when (kind) {
                CaptureKind.IMAGE -> "image_attachment.jpg"
                CaptureKind.VIDEO -> "video_attachment.mp4"
            }
            PlatformResult.Success(
                ExternalContentReference(getTempWritableAttachmentUri(context, cacheRoot / name).toString())
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            appLogger.e("Capture target could not be created", error)
            PlatformResult.Failure(error.message)
        }
    }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ContentKeyGenerator>())
class UuidContentKeyGenerator : ContentKeyGenerator {
    override fun nextKey(): String = UUID.randomUUID().toString()
}

fun ExternalContentReference.asAndroidUri(): Uri = Uri.parse(token)

fun Uri.asExternalContentReference(): ExternalContentReference = ExternalContentReference(toString())

internal fun Uri.requireExternalContentUri() {
    require(ContentResolver.SCHEME_CONTENT.equals(scheme, ignoreCase = true)) {
        "Only content URIs are supported"
    }
}
