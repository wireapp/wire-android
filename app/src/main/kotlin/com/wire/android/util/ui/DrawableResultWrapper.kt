package com.wire.android.util.ui

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import okio.Path
import okio.Source
import okio.buffer

internal class DrawableResultWrapper(val resources: Resources) {

    internal fun toFetchResult(assetPath: Path): FetchResult {
        return SourceResult(
            source = ImageSource(file = assetPath, diskCacheKey = assetPath.name), mimeType = null,
            dataSource = DataSource.DISK
        )
    }
}
