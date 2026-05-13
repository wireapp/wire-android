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

package com.wire.android.ui.home.conversations.composer

import com.wire.android.util.FileManager
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Inject

interface TempWritableAttachmentUriProvider {
    suspend fun getTempWritableVideoUri(): String
    suspend fun getTempWritableImageUri(): String
}

class AndroidTempWritableAttachmentUriProvider @Inject constructor(
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
) : TempWritableAttachmentUriProvider {
    override suspend fun getTempWritableVideoUri(): String =
        fileManager.getTempWritableVideoUri(kaliumFileSystem.rootCachePath).toString()

    override suspend fun getTempWritableImageUri(): String =
        fileManager.getTempWritableImageUri(kaliumFileSystem.rootCachePath).toString()
}

@Module
@InstallIn(ViewModelComponent::class)
interface TempWritableAttachmentUriProviderModule {
    @Binds
    fun bindTempWritableAttachmentUriProvider(
        provider: AndroidTempWritableAttachmentUriProvider
    ): TempWritableAttachmentUriProvider
}
