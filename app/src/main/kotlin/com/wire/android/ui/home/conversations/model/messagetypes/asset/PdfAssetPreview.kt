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
package com.wire.android.ui.home.conversations.model.messagetypes.asset

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.wire.android.util.ui.PdfPreviewDecoder

@Composable
fun PdfAssetPreview(
    assetPath: String,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val model = ImageRequest.Builder(context)
        .data(assetPath)
        .decoderFactory { result, options, _ ->
            PdfPreviewDecoder(result.source, options)
        }
        .build()

    AsyncImage(
        modifier = modifier.fillMaxSize(),
        model = model,
        contentScale = ContentScale.FillWidth,
        contentDescription = null,
    )
}
