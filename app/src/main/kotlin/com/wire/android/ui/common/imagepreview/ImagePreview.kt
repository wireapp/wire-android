package com.wire.android.ui.common.imagepreview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R


private val imagePreviewHeight = 360.dp

@Composable
fun ImagePreview(avatarUrl: String, contentDescription: String) {
    ConstraintLayout(
        Modifier
            .fillMaxWidth()
            .height(imagePreviewHeight)
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
            //TODO: fetch image, for now hard-coded
            Image(
                painter = painterResource(id = R.drawable.mock_message_image),
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
        //because we want the "bullet hole" to look circular instead of oval we need a perfect rectangle
        //following the max height of 360.dp, we subtract the width to match rectangle as a correctionFactor
        //after that we divide by 2 because we want to subtract it from each side
        val widthCorrectionFactor = with(density) {
            (size.width.toDp() - imagePreviewHeight).toPx() / 2
        }
        return Outline.Generic(
            drawBulletHolePath(widthCorrectionFactor, size)
        )
    }

    private fun drawBulletHolePath(widthCorrectionFactor: Float, size: Size): Path {
        val backgroundWrappingRect = size.toRect()

        val correctionRect = Rect(
            top = backgroundWrappingRect.top,
            bottom = backgroundWrappingRect.bottom,
            left = backgroundWrappingRect.left + widthCorrectionFactor,
            right = backgroundWrappingRect.right - widthCorrectionFactor
        )

        val path = Path().apply {
            reset()
            //move the origin point to the middle of the backgroundWrappingRect on the left side
            moveTo(x = 0f, y = size.height / 2)
            //draw a line to from the middle of backgroundWrappingRect to the top on the left side
            lineTo(x = 0f, y = 0f)
            //draw a line from the left edge to the right edge on the top side
            lineTo(x = size.width, y = 0f)
            //draw a line from the right edge to the middle of backgroundWrappingRect on the right side
            lineTo(x = size.width, y = size.height / 2)
            //arc -180 degrees from the start point of correctionRect -
            // we on the -180 degrees of the bullet hole circle made from correctionRect
            arcTo(correctionRect, 0f, -180f, true)
            //because we draw inside the correctionRect, we need to draw a line relative to current position
            //in order to move to the edge of the backgroundWrappingRect
            relativeLineTo(dx = -widthCorrectionFactor, dy = 0f)
            //draw a line from middle of backgroundWrappingRect to the bottom of backgroundWrappingRect on the left side
            lineTo(x = 0f, y = size.height)
            //draw a line from the bottom edge of backgroundWrappingRect to the right edge on the bottom side
            lineTo(x = size.width, y = size.height)
            //draw a line from the bottom edge of the backgroundWrappingRect to the middle of backgroundWrappingRect on the right side
            lineTo(x = size.width, y = size.height / 2)
            //arc 180 degrees - we are back on middle of the backgroundWrappingRect on the left side now
            arcTo(correctionRect, 0f, 180f, true)
            //we drawn the outline, we can close the path now
            close()
        }

        return path
    }

}
