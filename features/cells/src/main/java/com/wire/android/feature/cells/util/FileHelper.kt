/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.Path
import javax.inject.Inject

class FileHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun openAssetFileWithExternalApp(
        localPath: Path,
        assetName: String?,
        mimeType: String,
        onError: () -> Unit
    ) {
        try {
            val assetUri = context.pathToUri(localPath, assetName)

            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setDataAndType(assetUri, mimeType)
            }
            context.startActivity(intent)
        } catch (e: java.lang.IllegalArgumentException) {
            onError()
        } catch (noActivityFoundException: ActivityNotFoundException) {
            onError()
        }
    }

    fun openAssetUrlWithExternalApp(
        url: String,
        mimeType: String,
        onError: () -> Unit
    ) {
        try {
            val intent = Intent()
            intent.apply {
                action = Intent.ACTION_VIEW
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setDataAndType(Uri.parse(url), mimeType)
            }
            context.startActivity(intent)
        } catch (e: java.lang.IllegalArgumentException) {
            onError()
        } catch (noActivityFoundException: ActivityNotFoundException) {
            onError()
        }
    }

    fun shareFileChooser(assetDataPath: Path, assetName: String?, mimeType: String, onError: () -> Unit) {
        try {
            val assetUri = context.pathToUri(assetDataPath, assetName)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setDataAndType(assetUri, mimeType)
                putExtra(Intent.EXTRA_STREAM, assetUri)
            }
            val chooserIntent = Intent.createChooser(intent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: java.lang.IllegalArgumentException) {
            onError()
        } catch (noActivityFoundException: ActivityNotFoundException) {
            onError()
        }
    }

    fun shareUrlChooser(url: String, onError: () -> Unit) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                setType("text/plain")
                putExtra(Intent.EXTRA_TEXT, url)
            }
            val chooserIntent = Intent.createChooser(intent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: java.lang.IllegalArgumentException) {
            onError()
        } catch (noActivityFoundException: ActivityNotFoundException) {
            onError()
        }
    }

    private fun Context.getProviderAuthority() = "$packageName.provider"

    private fun Context.pathToUri(assetDataPath: Path, assetName: String?): Uri =
        FileProvider.getUriForFile(this, getProviderAuthority(), assetDataPath.toFile(), assetName ?: assetDataPath.name)
}
