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
package com.wire.android.pdfviewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import kotlin.math.abs
import kotlin.math.ceil

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f

/** Beyond this the extra pixels are no longer visible but the bitmaps get very expensive. */
private const val MAX_RENDER_SCALE = 3f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * Reusable full screen PDF viewer. Shows either a local file ([localPath]) or a remote asset
 * identified by [assetId] and [remotePath], which is fetched into the app cache before rendering.
 *
 * Callers own navigation via [onNavigateBack]; the ViewModel is resolved from the shared
 * pdf-viewer Metro graph so any module can host this screen.
 */
@Composable
fun PdfViewer(
    localPath: String?,
    assetId: String?,
    remotePath: String?,
    conversationId: String?,
    assetSize: Long,
    fileName: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfViewerViewModel = pdfViewerViewModel(localPath, assetId, remotePath, conversationId, assetSize, fileName),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PdfViewerContent(
        state = state,
        fileName = fileName,
        onRetry = viewModel::retry,
        onNavigateBack = onNavigateBack,
        renderPage = viewModel::renderPage,
        modifier = modifier,
    )
}

@Composable
internal fun PdfViewerContent(
    state: PdfViewerState,
    fileName: String?,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    renderPage: suspend (pageIndex: Int, widthPx: Int) -> Bitmap?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = fileName ?: stringResource(R.string.pdf_viewer_title),
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onNavigateBack,
                subtitleContent = {
                    if (state is PdfViewerState.Content) {
                        PageIndicator(listState = listState, pageCount = state.pageCount)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(colorsScheme().background),
        ) {
            when (state) {
                PdfViewerState.Loading -> CenteredCircularProgressBarIndicator()
                is PdfViewerState.Failure -> PdfViewerFailure(error = state.error, onRetry = onRetry)
                is PdfViewerState.Content -> PdfPages(
                    state = state,
                    listState = listState,
                    renderPage = renderPage,
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(listState: LazyListState, pageCount: Int) {
    val currentPage by remember(pageCount) {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount) }
    }
    Text(
        text = stringResource(R.string.pdf_viewer_page_indicator, currentPage, pageCount),
        style = typography().subline01,
        color = colorsScheme().secondaryText,
    )
}

@Composable
private fun PdfPages(
    state: PdfViewerState.Content,
    listState: LazyListState,
    renderPage: suspend (pageIndex: Int, widthPx: Int) -> Bitmap?,
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM) }
    var horizontalOffset by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableIntStateOf(0) }

    fun applyTransform(zoomChange: Float, panX: Float) {
        scale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        // Panning is only meaningful once the content is wider than the viewport.
        val maxOffset = viewportWidthPx * (scale - MIN_ZOOM) / 2f
        horizontalOffset = (horizontalOffset + panX).coerceIn(-maxOffset, maxOffset)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewportWidthPx = it.width }
            .zoomAndPan(currentScale = { scale }, onTransform = ::applyTransform)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > MIN_ZOOM) {
                            scale = MIN_ZOOM
                            horizontalOffset = 0f
                        } else {
                            scale = DOUBLE_TAP_ZOOM
                        }
                    },
                )
            },
    ) {
        // Pages are rasterised at the zoomed width so text stays sharp instead of being upscaled.
        val renderScale = ceil(scale).coerceIn(MIN_ZOOM, MAX_RENDER_SCALE)
        val pageWidthPx = (viewportWidthPx * renderScale).toInt()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = horizontalOffset
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
                },
            contentPadding = PaddingValues(dimensions().spacing8x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
        ) {
            items(count = state.pageCount, key = { it }) { pageIndex ->
                PdfPage(
                    pageIndex = pageIndex,
                    fallbackAspectRatio = state.firstPageAspectRatio,
                    widthPx = pageWidthPx,
                    renderPage = renderPage,
                )
            }
        }
    }
}

@Composable
private fun PdfPage(
    pageIndex: Int,
    fallbackAspectRatio: Float,
    widthPx: Int,
    renderPage: suspend (pageIndex: Int, widthPx: Int) -> Bitmap?,
) {
    // Deliberately keyed on the page only: while a sharper bitmap is rendered after a zoom the
    // previous one stays on screen instead of flashing back to a spinner.
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, widthPx) {
        if (widthPx > 0) {
            renderPage(pageIndex, widthPx)?.let { bitmap = it }
        }
    }

    val rendered = bitmap
    val aspectRatio = when {
        rendered != null && rendered.height > 0 -> rendered.width.toFloat() / rendered.height
        else -> fallbackAspectRatio
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (rendered != null) {
            Image(
                bitmap = rendered.asImageBitmap(),
                contentDescription = stringResource(
                    R.string.pdf_viewer_page_content_description,
                    pageIndex + 1,
                ),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            WireCircularProgressIndicator(
                progressColor = colorsScheme().secondaryText,
                size = dimensions().spacing32x,
            )
        }
    }
}

@Composable
private fun PdfViewerFailure(error: PdfViewerError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions().spacing24x),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(error.messageResId()),
            style = typography().body01,
            color = colorsScheme().onBackground,
            textAlign = TextAlign.Center,
        )
        if (error.isRetryable()) {
            WirePrimaryButton(
                onClick = onRetry,
                text = stringResource(R.string.pdf_viewer_retry),
                fillMaxWidth = false,
                modifier = Modifier.padding(top = dimensions().spacing16x),
            )
        }
    }
}

private fun PdfViewerError.messageResId(): Int = when (this) {
    PdfViewerError.FILE_NOT_FOUND -> R.string.pdf_viewer_error_file_not_found
    PdfViewerError.DOWNLOAD_FAILED -> R.string.pdf_viewer_error_download_failed
    PdfViewerError.PASSWORD_PROTECTED -> R.string.pdf_viewer_error_password_protected
    PdfViewerError.INVALID_DOCUMENT -> R.string.pdf_viewer_error_invalid_document
}

private fun PdfViewerError.isRetryable(): Boolean = this == PdfViewerError.DOWNLOAD_FAILED

/**
 * Pinch to zoom plus horizontal panning, layered on top of the list's own vertical scrolling.
 *
 * Events are inspected on [PointerEventPass.Initial] so a two finger pinch is claimed before the
 * list turns it into a scroll. Single finger gestures are only taken over when the content is
 * zoomed in *and* the drag is mostly horizontal, which leaves vertical scrolling to the list.
 */
private fun Modifier.zoomAndPan(
    currentScale: () -> Float,
    onTransform: (zoomChange: Float, panX: Float) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedPointers = event.changes.count { it.pressed }
            val pan = event.calculatePan()
            val handled = when {
                pressedPointers > 1 -> {
                    onTransform(event.calculateZoom(), pan.x)
                    true
                }

                currentScale() > MIN_ZOOM && abs(pan.x) > abs(pan.y) -> {
                    onTransform(1f, pan.x)
                    true
                }

                else -> false
            }
            if (handled) {
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

@MultipleThemePreviews
@Composable
fun PreviewPdfViewerLoading() = WireTheme {
    PdfViewerContent(
        state = PdfViewerState.Loading,
        fileName = "Quarterly report.pdf",
        onRetry = {},
        onNavigateBack = {},
        renderPage = { _, _ -> null },
    )
}

@MultipleThemePreviews
@Composable
fun PreviewPdfViewerFailure() = WireTheme {
    PdfViewerContent(
        state = PdfViewerState.Failure(PdfViewerError.DOWNLOAD_FAILED),
        fileName = "Quarterly report.pdf",
        onRetry = {},
        onNavigateBack = {},
        renderPage = { _, _ -> null },
    )
}
