package com.wire.android.util.extension

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

private const val BitMapQuality = 100

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, BitMapQuality, stream)

    return stream.toByteArray()
}
