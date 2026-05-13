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
package com.wire.android.feature.cells.ui.imageviewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wire.android.feature.cells.R
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireCellsDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme

@WireCellsDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = CellImageViewerNavArgs::class,
)
@Composable
fun CellImageViewerScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    viewModel: CellImageViewerViewModel = hiltViewModel(),
) {
    CellImageViewerScreenContent(
        localPath = viewModel.localPath,
        contentUrl = viewModel.contentUrl,
        previewUrl = viewModel.previewUrl,
        contentHash = viewModel.contentHash,
        fileName = viewModel.fileName,
        onNavigateBack = navigator::navigateBack,
        modifier = modifier,
    )
}

@Composable
internal fun CellImageViewerScreenContent(
    localPath: String?,
    contentUrl: String?,
    previewUrl: String?,
    contentHash: String?,
    fileName: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = fileName ?: stringResource(R.string.conversation_files_title),
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CellZoomableImage(
                localPath = localPath,
                contentUrl = contentUrl,
                previewUrl = previewUrl,
                contentHash = contentHash,
                contentDescription = fileName ?: stringResource(R.string.content_description_image_message),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CellZoomableImage(
    localPath: String?,
    contentUrl: String?,
    previewUrl: String?,
    contentHash: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    val minScale = 1.0f
    val maxScale = 3f

    val painter = when {
        localPath != null -> rememberAsyncImagePainter(localPath)
        contentUrl != null -> rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(contentUrl)
                .diskCacheKey(contentHash)
                .memoryCacheKey(contentHash)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            placeholder = previewUrl?.let {
                rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .diskCacheKey(contentHash)
                        .memoryCacheKey(contentHash)
                        .crossfade(true)
                        .build()
                )
            },
        )
        else -> return
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
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(minScale, maxScale)
                    if (zoom > 1) {
                        offsetX += pan.x * zoom
                        offsetY += pan.y * zoom
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentScale = ContentScale.Fit,
    )
}

@MultipleThemePreviews
@Composable
fun PreviewCellImageViewerScreen() {
    WireTheme {
        CellImageViewerScreenContent(
            localPath = null,
            contentUrl = null,
            previewUrl = null,
            contentHash = null,
            fileName = "photo.jpg",
            onNavigateBack = {},
        )
    }
}
