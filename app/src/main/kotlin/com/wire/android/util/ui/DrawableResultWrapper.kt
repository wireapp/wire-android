package com.wire.android.util.ui

import android.content.res.Resources
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import okio.Path

internal class DrawableResultWrapper(val resources: Resources) {

    internal fun toFetchResult(assetPath: Path): FetchResult {
        return SourceResult(
            source = ImageSource(file = assetPath, diskCacheKey = assetPath.name), mimeType = null,
            dataSource = DataSource.DISK
        )
    }
}
