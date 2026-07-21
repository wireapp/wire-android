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

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.FileProvider
import java.io.File
import java.nio.file.Files

fun Context.externalShareChooserIntent(
    sendIntent: Intent,
    title: CharSequence? = null,
    excludeOwnComponents: Boolean = true
): Intent =
    Intent.createChooser(sendIntent, title).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!excludeOwnComponents) return@apply
        val ownShareComponents = packageManager.queryIntentActivitiesCompat(sendIntent)
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
            .filter { it.packageName == packageName }
            .toTypedArray()
        if (ownShareComponents.isNotEmpty()) {
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, ownShareComponents)
        }
    }

fun Context.startShareIntentWithTrustedWireTarget(sendIntent: Intent, title: CharSequence? = null) {
    val chooserIntent = externalShareChooserIntent(
        sendIntent = sendIntent,
        title = title,
        excludeOwnComponents = !supportsTrustedWireShareCaller()
    )
    startActivity(chooserIntent, shareIdentityOptionsBundle())
}

fun supportsTrustedWireShareCaller(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

private fun shareIdentityOptionsBundle(): Bundle? =
    if (supportsTrustedWireShareCaller()) {
        ActivityOptions.makeBasic()
            .setShareIdentityEnabled(true)
            .toBundle()
    } else {
        null
    }

fun Context.fileProviderSharedCacheFile(fileName: String): File {
    val shareDirectory = fileProviderSharedCacheDirectory()
    deleteStaleFileProviderSharedCacheFiles(shareDirectory)
    return File(shareDirectory, findFirstUniqueName(shareDirectory, fileName.ifBlank { ATTACHMENT_FILENAME }))
}

fun Context.shareableFileProviderUri(sourceFile: File, displayName: String? = null): Uri {
    val shareFile = when {
        sourceFile.isInDirectory(fileProviderSharedCacheDirectory()) -> sourceFile
        else -> linkOrCopyToFileProviderSharedCache(sourceFile, displayName ?: sourceFile.name)
    }
    return fileProviderUri(shareFile, displayName)
}

private fun Context.linkOrCopyToFileProviderSharedCache(sourceFile: File, displayName: String): File {
    val shareFile = fileProviderSharedCacheFile(displayName)
    runCatching {
        Files.createLink(shareFile.toPath(), sourceFile.toPath())
    }.recoverCatching {
        sourceFile.copyTo(shareFile, overwrite = true)
    }.getOrThrow()
    return shareFile
}

private fun Context.fileProviderSharedCacheDirectory(): File =
    File(cacheDir, FILE_PROVIDER_SHARED_CACHE_DIRECTORY).apply { mkdirs() }

fun Context.fileProviderUri(file: File, displayName: String? = null): Uri =
    FileProvider.getUriForFile(this, getProviderAuthority(), file, displayName ?: file.name)

private fun File.isInDirectory(directory: File): Boolean {
    val directoryPath = directory.canonicalFile.toPath()
    return canonicalFile.toPath().startsWith(directoryPath)
}

private fun deleteStaleFileProviderSharedCacheFiles(directory: File) {
    val oldestAllowedTimestamp = System.currentTimeMillis() - FILE_PROVIDER_SHARED_CACHE_MAX_AGE_MILLIS
    directory.listFiles()
        ?.filter { it.isFile && it.lastModified() < oldestAllowedTimestamp }
        ?.forEach(File::delete)
}

private fun PackageManager.queryIntentActivitiesCompat(intent: Intent) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

fun Context.getProviderAuthority() = "$packageName.provider"

const val FILE_PROVIDER_SHARED_FILES_ROOT = "shared_files"
private const val FILE_PROVIDER_SHARED_CACHE_DIRECTORY = "file-provider-shares"
private const val FILE_PROVIDER_SHARED_CACHE_MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L
private const val ATTACHMENT_FILENAME = "attachment"
