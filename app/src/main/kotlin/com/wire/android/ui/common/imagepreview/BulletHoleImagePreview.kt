package com.wire.android.ui.common.imagepreview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import kotlinx.coroutines.NonDisposableHandle.parent

@Composable
fun BulletHoleImagePreview(imageBitmap: Bitmap, contentDescription: String) {
    ConstraintLayout(
        Modifier
            .aspectRatio(1f)
            .height(dimensions().imagePreviewHeight)
    ) {
        val (avatarImage, semiTransparentBackground) = createRefs()
        Box(
            Modifier
                .fillMaxSize()
                .constrainAs(avatarImage) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            Image(
                bitmap = imageBitmap.asImageBitmap(),
                contentScale = ContentScale.Crop,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .constrainAs(semiTransparentBackground) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
                .graphicsLayer(
                    shape = BulletHoleShape(),
                    alpha = 0.65f,
                    clip = true
                )
                .background(color = MaterialTheme.colorScheme.surface)
        )
    }
}

//Custom Shape creating a "hole" around the shape of the provided Composable
//in case of ImagePreview that would be a rectangular shape, creating an effect of the "hole" around the rectangle
@Suppress("MagicNumber")
class BulletHoleShape : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(
            drawBulletHolePath(size)
        )
    }

    private fun drawBulletHolePath(size: Size): Path {
        val backgroundWrappingRect = size.toRect()

        val path = Path().apply {
            reset()
            //move the origin point to the middle of the backgroundWrappingRect on the left side
            moveTo(x = 0f, y = backgroundWrappingRect.height / 2)
            //draw a line to from the middle of backgroundWrappingRect to the top on the left side
            lineTo(x = 0f, y = 0f)
            //draw a line from the left edge to the right edge on the top side
            lineTo(x = backgroundWrappingRect.width, y = 0f)
            //draw a backgroundWrappingRect from the right edge to the middle of backgroundWrappingRect on the right side
            lineTo(x = size.width, y = backgroundWrappingRect.height / 2)
            //arc -180 degrees from the start point of backgroundWrappingRect -
            arcTo(backgroundWrappingRect, 0f, -180f, true)
            //draw a line from middle of backgroundWrappingRect to the bottom of backgroundWrappingRect on the left side
            lineTo(x = 0f, y = backgroundWrappingRect.height)
            //draw a line from the bottom edge of backgroundWrappingRect to the right edge on the bottom side
            lineTo(x = backgroundWrappingRect.width, y = backgroundWrappingRect.height)
            //draw a line from the bottom edge of the backgroundWrappingRect to the middle of backgroundWrappingRect on the right side
            lineTo(x = backgroundWrappingRect.width, y = backgroundWrappingRect.height / 2)
            //arc 180 degrees - we are back on middle of the backgroundWrappingRect on the left side now
            arcTo(backgroundWrappingRect, 0f, 180f, true)
            //we drawn the outline, we can close the path now
            close()
        }

        return path
    }

}
