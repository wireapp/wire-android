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

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun ZoomableImage(image: MediaGalleryImage, contentDescription: String, modifier: Modifier = Modifier) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    val minScale = 1.0f
    val maxScale = 3f

    val painter = when (image) {
        is MediaGalleryImage.PrivateAsset -> image.asset.paint()
        is MediaGalleryImage.LocalAsset -> rememberAsyncImagePainter(image.path)
        is MediaGalleryImage.UrlAsset -> rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(image.url)
                .diskCacheKey(image.contentHash)
                .memoryCacheKey(image.contentHash)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            placeholder = image.placeholder?.let {
                rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .diskCacheKey(image.contentHash)
                        .memoryCacheKey(image.contentHash)
                        .data(it)
                        .crossfade(true)
                        .build()
                )
            }
        )
    }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .graphicsLayer(
                scaleX = zoom,
                scaleY = zoom,
                translationX = offsetX,
                translationY = offsetY,
            )
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(minScale, maxScale)
                        if (zoom > 1) {
                            offsetX += pan.x * zoom
                            offsetY += pan.y * zoom
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                )
            }
            .fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}
