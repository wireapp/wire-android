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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wire.android.appLogger
import com.wire.android.ui.common.image.ZoomableImageContainer

private const val PREVIEW_CACHE_KEY_SUFFIX = "-preview"

@Composable
fun ZoomableImage(
    image: MediaGalleryImage,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onRenderFailed: () -> Unit = {},
) {
    val context = LocalContext.current

    // Only network/file backed images report failures here. PrivateAsset goes through
    // WireSessionImageLoader, which already owns its own retry policy.
    var asyncPainter: AsyncImagePainter? = null

    val painter = when (image) {
        is MediaGalleryImage.PrivateAsset -> image.asset.paint()

        is MediaGalleryImage.LocalAsset ->
            rememberAsyncImagePainter(image.path).also { asyncPainter = it }

        is MediaGalleryImage.UrlAsset ->
            rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(image.url)
                    .diskCacheKey(image.contentHash)
                    .memoryCacheKey(image.contentHash)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                placeholder = image.placeholder?.takeIf { it != image.url }?.let {
                    rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(it)
                            .diskCacheKey(image.contentHash?.let { hash -> "$hash$PREVIEW_CACHE_KEY_SUFFIX" })
                            .memoryCacheKey(image.contentHash?.let { hash -> "$hash$PREVIEW_CACHE_KEY_SUFFIX" })
                            .crossfade(true)
                            .build()
                    )
                }
            ).also { asyncPainter = it }
    }

    LaunchedEffect(asyncPainter) {
        asyncPainter?.state?.collect { state ->
            if (state is AsyncImagePainter.State.Error) {
                appLogger.e("ZoomableImage failed to load ${image::class.simpleName}", state.result.throwable)
                onRenderFailed()
            }
        }
    }

    ZoomableImageContainer(
        painter = { painter },
        contentDescription = contentDescription,
        modifier = modifier
    )
}
