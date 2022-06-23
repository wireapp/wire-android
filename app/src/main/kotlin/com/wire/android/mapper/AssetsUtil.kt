package com.wire.android.mapper

internal fun isImage(mimeType: String): Boolean = when (mimeType) {
    // We only support the following inline image types
    "image/jpeg" -> true
    "image/jpg" -> true
    "image/png" -> true
    else -> false
}
