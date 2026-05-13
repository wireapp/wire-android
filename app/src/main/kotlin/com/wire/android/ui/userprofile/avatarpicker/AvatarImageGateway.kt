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

package com.wire.android.ui.userprofile.avatarpicker

import android.content.Context
import androidx.core.net.toUri
import com.wire.android.util.AvatarImageManager
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.android.util.toByteArray
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.Path
import javax.inject.Inject

interface AvatarImageGateway {
    fun getWritableAvatarUri(avatarPath: Path): String
    fun getShareableTempAvatarUri(avatarPath: Path): String
    suspend fun sanitizeAvatarImage(originalAvatarUri: String, avatarPath: Path)
    suspend fun getAvatarImageSize(avatarUri: String): Long
}

class AndroidAvatarImageGateway @Inject constructor(
    private val avatarImageManager: AvatarImageManager,
    private val dispatchers: DispatcherProvider,
    @ApplicationContext private val appContext: Context
) : AvatarImageGateway {

    override fun getWritableAvatarUri(avatarPath: Path): String =
        avatarImageManager.getWritableAvatarUri(avatarPath).toString()

    override fun getShareableTempAvatarUri(avatarPath: Path): String =
        avatarImageManager.getShareableTempAvatarUri(avatarPath).toString()

    /**
     * Resamples the image and removes unnecessary metadata before uploading it.
     * This avoids uploading unnecessarily large profile pictures and sensitive metadata.
     */
    override suspend fun sanitizeAvatarImage(originalAvatarUri: String, avatarPath: Path) {
        originalAvatarUri.toUri().resampleImageAndCopyToTempPath(
            context = appContext,
            tempCachePath = avatarPath,
            sizeClass = ImageUtil.ImageSizeClass.Small,
            shouldRemoveMetadata = true
        )
    }

    override suspend fun getAvatarImageSize(avatarUri: String): Long =
        avatarUri.toUri().toByteArray(appContext, dispatchers).size.toLong()
}

@Module
@InstallIn(ViewModelComponent::class)
interface AvatarImageGatewayModule {
    @Binds
    fun bindAvatarImageGateway(gateway: AndroidAvatarImageGateway): AvatarImageGateway
}
