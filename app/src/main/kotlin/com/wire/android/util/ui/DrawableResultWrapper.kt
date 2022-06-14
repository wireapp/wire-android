package com.wire.android.util.ui

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import okio.Path
import okio.buffer

internal class DrawableResultWrapper(val resources: Resources, val kaliumFileSystem: KaliumFileSystem) {

    internal fun toFetchResult(decodedAssetPath: Path): FetchResult = DrawableResult(
        BitmapDrawable(resources, kaliumFileSystem.source(decodedAssetPath).buffer().inputStream()), false, DataSource.DISK
    )
}
