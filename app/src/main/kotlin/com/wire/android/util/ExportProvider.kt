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
package com.wire.android.util

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.wire.android.appLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.FileNotFoundException
import java.net.URLConnection
import java.util.UUID

fun Context.getExportProviderAuthority() = "$packageName.exportprovider"

fun Context.stagePathForExternalExport(assetDataPath: Path, assetName: String?): Uri =
    exportStore().stageFile(
        sourceFile = assetDataPath.toFile(),
        displayName = assetName,
        mimeType = exportMimeType(assetName ?: assetDataPath.name, Uri.fromFile(assetDataPath.toFile()).getMimeType(this))
    )

fun Context.stageUriForExternalExport(uri: Uri, assetName: String? = null): Uri =
    exportStore().stageUri(uri, assetName)

fun Context.stageFilesForExternalExport(files: List<File>): ArrayList<Uri> {
    val exportedUris = ArrayList<Uri>()
    files.forEach { file ->
        runCatching {
            stagePathForExternalExport(file.toOkioPath(), file.name)
        }.onSuccess(exportedUris::add)
            .onFailure { error ->
                appLogger.e("Failed to stage file ${file.absolutePath} for external export", error)
            }
    }
    return exportedUris
}

private fun Context.exportStore() = ExportStore(applicationContext ?: this)

class ExportProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = context?.exportStore()?.getRecord(uri)?.let { record ->
        val columns = projection ?: DEFAULT_PROJECTION
        MatrixCursor(columns).apply {
            addRow(
                columns.map<_, Any?> { column ->
                    when (column) {
                        OpenableColumns.DISPLAY_NAME -> record.entry.fileName
                        OpenableColumns.SIZE -> record.file.length()
                        else -> null
                    }
                }.toTypedArray()
            )
        }
    }

    override fun getType(uri: Uri): String? = context?.exportStore()?.getRecord(uri)?.entry?.let { entry ->
        exportMimeType(entry.fileName, entry.mimeType)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == READ_ONLY_MODE) { "Exported files are read-only" }
        val context = context ?: throw FileNotFoundException("Provider context is unavailable")
        return context.exportStore().openFile(uri)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        private val DEFAULT_PROJECTION = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        private const val READ_ONLY_MODE = "r"
    }
}

private class ExportStore(private val context: Context) {

    fun stageFile(sourceFile: File, displayName: String?, mimeType: String?): Uri {
        require(sourceFile.exists()) { "The file couldn't be found on the internal storage" }

        cleanUpExpiredExports()

        val fileName = sanitizeExportDisplayName(displayName ?: sourceFile.name)
        val token = UUID.randomUUID().toString()
        val stagedFile = File(exportFilesDirectory(context), exportStagedFileName(token, fileName, mimeType))
        sourceFile.copyTo(stagedFile, overwrite = true)
        writeManifest(
            token = token,
            entry = ExportEntry(
                fileName = fileName,
                mimeType = mimeType,
                stagedFileName = stagedFile.name,
            )
        )
        return buildUri(token)
    }

    fun stageUri(sourceUri: Uri, assetName: String?): Uri {
        cleanUpExpiredExports()

        val fileName = sanitizeExportDisplayName(assetName ?: context.getFileName(sourceUri))
        val mimeType = exportMimeType(fileName, sourceUri.getMimeType(context))
        val token = UUID.randomUUID().toString()
        val stagedFile = File(exportFilesDirectory(context), exportStagedFileName(token, fileName, mimeType))
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            stagedFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw FileNotFoundException("Unable to open shared content: $sourceUri")
        writeManifest(
            token = token,
            entry = ExportEntry(
                fileName = fileName,
                mimeType = mimeType,
                stagedFileName = stagedFile.name,
            )
        )
        return buildUri(token)
    }

    fun getRecord(uri: Uri): ExportRecord? = tokenFromUri(uri)?.let { token ->
        readManifest(token)?.let { entry ->
            File(exportFilesDirectory(context), entry.stagedFileName)
                .takeIf(File::exists)
                ?.let { file -> ExportRecord(file, entry) }
                ?: run {
                    exportManifestFile(context, token).delete()
                    null
                }
        }
    }

    fun openFile(uri: Uri): ParcelFileDescriptor {
        val record = getRecord(uri) ?: throw FileNotFoundException("Unable to resolve exported file for $uri")
        return ParcelFileDescriptor.open(record.file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun buildUri(token: String): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(context.getExportProviderAuthority())
        .appendPath(EXPORT_URI_SEGMENT)
        .appendPath(token)
        .build()

    private fun tokenFromUri(uri: Uri): String? {
        if (uri.authority != context.getExportProviderAuthority()) {
            return null
        }
        val pathSegments = uri.pathSegments
        return pathSegments
            .takeIf { it.size == 2 && it.first() == EXPORT_URI_SEGMENT }
            ?.get(1)
    }

    private fun writeManifest(token: String, entry: ExportEntry) {
        exportManifestsDirectory(context).mkdirs()
        exportManifestFile(context, token).writeText(Json.encodeToString(ExportEntry.serializer(), entry))
    }

    private fun readManifest(token: String): ExportEntry? = runCatching {
        Json.decodeFromString(ExportEntry.serializer(), exportManifestFile(context, token).readText())
    }.getOrNull()

    private fun cleanUpExpiredExports() {
        val expirationThreshold = System.currentTimeMillis() - EXPORT_MAX_AGE_IN_MILLIS

        exportManifestsDirectory(context).listFiles().orEmpty().forEach { manifest ->
            if (manifest.lastModified() < expirationThreshold) {
                val token = manifest.nameWithoutExtension
                readManifest(token)?.let { entry ->
                    File(exportFilesDirectory(context), entry.stagedFileName).delete()
                }
                manifest.delete()
            }
        }

        exportFilesDirectory(context).listFiles().orEmpty()
            .filter { it.lastModified() < expirationThreshold }
            .forEach(File::delete)
    }
}

private data class ExportRecord(
    val file: File,
    val entry: ExportEntry,
)

@Serializable
private data class ExportEntry(
    val fileName: String,
    val mimeType: String?,
    val stagedFileName: String,
)

private const val EXPORT_ROOT_DIR = "external-export"
private const val EXPORT_FILES_DIR = "files"
private const val EXPORT_MANIFESTS_DIR = "manifests"
private const val EXPORT_URI_SEGMENT = "attachment"
private const val EXPORT_MAX_AGE_IN_MILLIS = 60 * 60 * 1000L
private const val DEFAULT_EXPORT_FILE_NAME = "attachment"

private fun sanitizeExportDisplayName(displayName: String?): String =
    displayName
        ?.normalizeFileName()
        ?.sanitizeFilename()
        ?.ifBlank { DEFAULT_EXPORT_FILE_NAME }
        ?: DEFAULT_EXPORT_FILE_NAME

private fun exportStagedFileName(token: String, displayName: String, mimeType: String?): String {
    val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
        ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
    return if (extension.isBlank()) token else "$token.$extension"
}

private fun exportManifestsDirectory(context: Context): File =
    File(context.cacheDir, "$EXPORT_ROOT_DIR/$EXPORT_MANIFESTS_DIR").apply {
        mkdirs()
    }

private fun exportFilesDirectory(context: Context): File =
    File(context.cacheDir, "$EXPORT_ROOT_DIR/$EXPORT_FILES_DIR").apply {
        mkdirs()
    }

private fun exportManifestFile(context: Context, token: String): File =
    File(exportManifestsDirectory(context), "$token.json")

private fun exportMimeType(fileName: String, fallbackMimeType: String?): String? =
    fallbackMimeType
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        )
        ?: URLConnection.guessContentTypeFromName(fileName)
