package com.wire.android.util

import android.content.Context
import android.net.Uri
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okio.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun saveToExternalStorage(
        assetName: String,
        assetDataPath: Path,
        assetDataSize: Long,
        onFileSaved: suspend () -> Unit
    ) {
        saveFileToDownloadsFolder(assetName, assetDataPath, assetDataSize, context)
        onFileSaved()
    }

    fun openWithExternalApp(assetDataPath: Path, assetExtension: String?, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetDataPath, context, assetExtension, onError)
    }

    fun shareWithExternalApp(assetDataPath: Path, assetExtension: String?, onError: () -> Unit) {
        shareAssetFileWithExternalApp(assetDataPath, context, assetExtension, onError)
    }

    suspend fun copyToTempPath(uri: Uri, tempCachePath: Path, dispatcher: DispatcherProvider = DefaultDispatcherProvider()): Long =
        withContext(dispatcher.io()) {
            val file = tempCachePath.toFile()
            var size: Long
            file.setWritable(true)
            context.contentResolver.openInputStream(uri).use { inputStream ->
                file.outputStream().use {
                    size = inputStream?.copyTo(it) ?: -1L
                }
            }
            return@withContext size
        }
}
