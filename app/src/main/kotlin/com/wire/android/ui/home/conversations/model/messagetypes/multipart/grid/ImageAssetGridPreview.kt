/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart.grid

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel

@Composable
internal fun ImageAssetGridPreview(item: MultipartAttachmentUi) {
    if (LocalInspectionMode.current) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(com.wire.android.ui.common.R.drawable.mock_image),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    } else if (item.previewAvailable()) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = item.previewImageModel(),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }
}
