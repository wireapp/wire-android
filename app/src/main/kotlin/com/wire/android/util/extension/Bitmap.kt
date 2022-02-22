package com.wire.android.util.extension

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream


fun Bitmap.toByteArray(): ByteArray {
    val bitMapQuality = 100

    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, bitMapQuality, stream)

    return stream.toByteArray()
}
