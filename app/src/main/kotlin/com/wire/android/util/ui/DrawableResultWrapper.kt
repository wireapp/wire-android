package com.wire.android.util.ui

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import okio.Source
import okio.buffer

internal class DrawableResultWrapper(val resources: Resources) {

    internal fun toFetchResult(decodedAssetSource: Source): FetchResult {
        val decodedDrawable = BitmapDrawable(resources, decodedAssetSource.buffer().inputStream())
        return DrawableResult(decodedDrawable, false, DataSource.DISK)
    }
}
