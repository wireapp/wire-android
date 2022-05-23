package com.wire.android.ui.home.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.wire.android.model.ImageAsset
import com.wire.android.util.ui.WireSessionImageLoader

@Composable
fun ZoomableImage(imageAsset: ImageAsset, contentDescription: String, imageLoader: WireSessionImageLoader, imageScale: Float = 1.0f) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    val minScale = 1.0f
    val maxScale = 3f

    Box {
        Image(
            painter = imageLoader.paint(imageAsset),
            contentDescription = contentDescription,
            modifier = Modifier.align(Alignment.Center)
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
}
