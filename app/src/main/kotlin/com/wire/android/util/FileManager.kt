package com.wire.android.util

import android.content.Context
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun saveToExternalStorage(
        assetDataPath: Path,
        assetDataSize: Long,
        onFileSaved: suspend () -> Unit
    ) {
        saveFileToDownloadsFolder(assetDataPath, assetDataSize, context)
        onFileSaved()
    }

    fun openWithExternalApp(assetDataPath: Path, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetDataPath, context, onError)
    }
}
