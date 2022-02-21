package com.wire.android.ui.common.imagepreview

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wire.android.ui.userprofile.image.ImageSource
import com.wire.android.util.permission.OpenGalleryFlow
import com.wire.android.util.permission.TakePictureFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberTakePictureFlow

class PicturePickerFlow(
    val bitmapState: BitmapState,
    private val takePictureFlow: TakePictureFlow,
    private val openGalleryFlow: OpenGalleryFlow
) {
    fun launch(imageSource: ImageSource) {
        when (imageSource) {
            ImageSource.Camera -> takePictureFlow.launch()
            ImageSource.Gallery -> openGalleryFlow.launch()
        }
    }
}

@Composable
fun rememberPickPictureState(initialBitmap: Bitmap): PicturePickerFlow {
    val context = LocalContext.current

    var pickedPicture: BitmapState by remember {
        mutableStateOf(BitmapState.InitialBitmap(initialBitmap))
    }

    val takePictureFLow = rememberTakePictureFlow({ nullableBitmap ->
        nullableBitmap?.let {
            pickedPicture = BitmapState.BitmapPicked(it)
        }
    }, { })

    val openGalleryFlow = rememberOpenGalleryFlow({
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 29) {
            val source = ImageDecoder.createSource(context.contentResolver, it)

            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        }

        pickedPicture = BitmapState.BitmapPicked(bitmap)
    }, { })

    return remember(pickedPicture) {
        PicturePickerFlow(pickedPicture, takePictureFLow, openGalleryFlow)
    }
}

sealed class BitmapState(open val bitmap: Bitmap) {
    data class InitialBitmap(override val bitmap: Bitmap) : BitmapState(bitmap)
    data class BitmapPicked(override val bitmap: Bitmap) : BitmapState(bitmap)
}
