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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.wire.android.feature.cells.domain.model.icon
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewAvailable
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.previewImageModel
import com.wire.android.util.ui.PdfPreviewDecoder

@Composable
internal fun PdfAssetGridPreview(item: MultipartAttachmentUi) {
    if (item.previewAvailable()) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = item.previewImageModel(
                decoderFactory = { result, options, _ ->
                    PdfPreviewDecoder(result.source, options)
                }
            ),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }

    Box(
        modifier = Modifier.padding(dimensions().spacing8x),
    ) {
        Image(
            modifier = Modifier.size(dimensions().spacing16x),
            painter = painterResource(id = item.assetType.icon()),
            contentDescription = null,
        )
    }
}
