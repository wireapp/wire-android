package com.wire.android.util

import android.content.Context

interface FileManager {
    fun saveToExternalStorage(assetName: String?, assetData: ByteArray, onFileSaved: (String?) -> Unit)
    fun openWithExternalApp(assetName: String?, assetData: ByteArray, onError: () -> Unit)
}

class FileManagerImpl(private val context: Context) : FileManager {
    override fun saveToExternalStorage(assetName: String?, assetData: ByteArray, onFileSaved: (String?) -> Unit) {
        saveFileToDownloadsFolder(assetName, assetData, context)
        onFileSaved(assetName)
    }

    override fun openWithExternalApp(assetName: String?, assetData: ByteArray, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetName, assetData, context, onError)
    }
}
