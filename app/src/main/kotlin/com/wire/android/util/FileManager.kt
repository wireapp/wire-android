package com.wire.android.util

import android.content.Context
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import okio.Path

class FileManager(private val context: Context) {
    suspend fun saveToExternalStorage(assetName: String, assetDataPath: Path, assetDataSize: Long, kaliumFileSystem: KaliumFileSystem, onFileSaved: suspend (String?) -> Unit) {
        saveFileToDownloadsFolder(assetName, assetDataPath, assetDataSize, context, kaliumFileSystem)
        onFileSaved(assetName)
    }

    fun openWithExternalApp(assetName: String, assetDataPath: Path, kaliumFileSystem: KaliumFileSystem, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetName, assetDataPath, context, kaliumFileSystem, onError)
    }
}
