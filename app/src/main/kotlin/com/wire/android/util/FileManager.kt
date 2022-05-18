package com.wire.android.util

import android.content.Context

class FileManager(private val context: Context) {
    fun saveToExternalStorage(assetName: String?, assetData: ByteArray, onFileSaved: (String?) -> Unit) {
        saveFileToDownloadsFolder(assetName, assetData, context)
        onFileSaved(assetName)
    }

    fun openWithExternalApp(assetName: String?, assetData: ByteArray, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetName, assetData, context, onError)
    }
}
