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

package com.wire.android.ui.home.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wire.android.ui.common.image.ZoomableImageContainer

@Composable
fun ZoomableImage(
    image: MediaGalleryImage,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val painter = when (image) {
        is MediaGalleryImage.PrivateAsset -> image.asset.paint()

        is MediaGalleryImage.LocalAsset ->
            rememberAsyncImagePainter(image.path)

        is MediaGalleryImage.UrlAsset ->
            rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(image.url)
                    .diskCacheKey(image.contentHash)
                    .memoryCacheKey(image.contentHash)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                placeholder = image.placeholder?.let {
                    rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(it)
                            .diskCacheKey(image.contentHash)
                            .memoryCacheKey(image.contentHash)
                            .crossfade(true)
                            .build()
                    )
                }
            )
    }

    ZoomableImageContainer(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
    )
}
