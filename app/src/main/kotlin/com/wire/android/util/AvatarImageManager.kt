/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import okio.Path
import javax.inject.Inject

class AvatarImageManager @Inject constructor(val context: Context) {

    fun getWritableAvatarUri(imageDataPath: Path): Uri {
        val file = imageDataPath.toFile()
        return file.toUri()
    }

    fun getShareableTempAvatarUri(filePath: Path): Uri {
        return FileProvider.getUriForFile(context, context.getProviderAuthority(), filePath.toFile())
    }
}
