package com.wire.android.ui.home.conversations

import android.content.Context
import com.wire.android.util.openAssetFileWithExternalApp
import com.wire.android.util.saveFileToDownloadsFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface FileManager {
    fun saveToExternalStorage(assetName: String?, assetData: ByteArray)
    fun openWithExternalApp(assetName: String?, assetData: ByteArray, onError: () -> Unit)
}

class FileManagerImpl(private val context: Context, private val scope: CoroutineScope, private val onFileSaved: (String?) -> Unit) :
    FileManager {
    override fun saveToExternalStorage(assetName: String?, assetData: ByteArray) {
        scope.launch {
            withContext(Dispatchers.IO) {
                saveFileToDownloadsFolder(assetName, assetData, context)
                onFileSaved(assetName)
            }
        }
    }

    override fun openWithExternalApp(assetName: String?, assetData: ByteArray, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetName, assetData, context, onError)
    }
}
