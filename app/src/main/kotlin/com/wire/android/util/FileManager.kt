package com.wire.android.util

import android.content.Context
import com.wire.android.util.dispatchers.DispatcherProvider

class FileManager(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    suspend fun saveToExternalStorage(assetName: String?, assetData: ByteArray, onFileSaved: suspend (String?) -> Unit) {
        with(dispatchers.io()) {
            saveFileToDownloadsFolder(assetName, assetData, context)
            onFileSaved(assetName)
        }
    }

    fun openWithExternalApp(assetName: String?, assetData: ByteArray, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetName, assetData, context, onError)
    }
}
