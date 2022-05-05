package com.wire.android.util.ui

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import com.wire.android.util.toBitmap

internal class DrawableResultWrapper(val resources: Resources) {

    internal fun toFetchResult(decodedAsset: ByteArray): FetchResult = DrawableResult(
        BitmapDrawable(resources, decodedAsset.toBitmap()), false, DataSource.DISK
    )
}
