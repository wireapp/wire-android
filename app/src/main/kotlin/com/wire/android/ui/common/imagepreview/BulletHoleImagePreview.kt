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

package com.wire.android.ui.common.imagepreview

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.toBitmap
import com.wire.android.util.ui.PreviewMultipleThemesForLandscape
import com.wire.android.util.ui.PreviewMultipleThemesForPortrait
import com.wire.android.util.ui.PreviewMultipleThemesForSquare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
private fun loadBitMap(imageUri: Uri): State<Bitmap?> {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, imageUri) {
        value = withContext(Dispatchers.IO) {
            imageUri.toBitmap(context)
        }
    }
}

@Composable
fun BulletHoleImagePreview(
    imageUri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier,
    scrimColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = when {
                LocalInspectionMode.current -> painterResource(id = R.drawable.ic_create_team_success)
                else -> rememberAsyncImagePainter(loadBitMap(imageUri).value)
            },
            contentScale = FillCenterSquare,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent { // this cuts out the center circle
                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)
                        drawContent()
                        drawCircle(
                            color = Color.Black,
                            radius = min(drawContext.size.width, drawContext.size.height) / 2f,
                            center = drawContext.size.center,
                            blendMode = BlendMode.Clear
                        )
                        restoreToCount(checkPoint)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = scrimColor)
            )
        }
    }
}

@Stable
private val FillCenterSquare = object : ContentScale { // this scales the image to fill the center square
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        val squareSize = min(dstSize.width, dstSize.height)
        val scaleFactor = squareSize / min(srcSize.width, srcSize.height)
        return ScaleFactor(scaleFactor, scaleFactor)
    }
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun BulletHoleImagePreviewPreview() = WireTheme {
    BulletHoleImagePreview(imageUri = "".toUri(), contentDescription = "")
}
