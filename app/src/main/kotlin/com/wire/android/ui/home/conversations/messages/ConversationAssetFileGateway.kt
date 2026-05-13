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

package com.wire.android.ui.home.conversations.messages

import com.wire.android.util.FileManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import okio.Path
import javax.inject.Inject

interface ConversationAssetFileGateway {
    fun openWithExternalApp(assetDataPath: Path, assetName: String?, onError: () -> Unit)
    suspend fun saveToExternalStorage(assetName: String, assetDataPath: Path, assetSize: Long): String?
    fun shareWithExternalApp(assetDataPath: Path, assetName: String?)
}

class AndroidConversationAssetFileGateway @Inject constructor(
    private val fileManager: FileManager,
) : ConversationAssetFileGateway {

    override fun openWithExternalApp(assetDataPath: Path, assetName: String?, onError: () -> Unit) {
        fileManager.openWithExternalApp(assetDataPath, assetName, onError)
    }

    override suspend fun saveToExternalStorage(assetName: String, assetDataPath: Path, assetSize: Long): String? {
        var savedFileName: String? = null
        fileManager.saveToExternalStorage(assetName, assetDataPath, assetSize) {
            savedFileName = it
        }
        return savedFileName
    }

    override fun shareWithExternalApp(assetDataPath: Path, assetName: String?) {
        fileManager.shareWithExternalApp(assetDataPath, assetName) {}
    }
}

@Module
@InstallIn(ViewModelComponent::class)
interface ConversationAssetFileGatewayModule {
    @Binds
    fun bindConversationAssetFileGateway(gateway: AndroidConversationAssetFileGateway): ConversationAssetFileGateway
}
