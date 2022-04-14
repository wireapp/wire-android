package com.wire.android.util.ui

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.wire.android.model.UserAvatarAsset
import com.wire.android.util.toBitmap
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult

class AssetImageFetcher(
    val data: UserAvatarAsset,
    val getAvatarAsset: GetAvatarAssetUseCase,
    val resources: Resources,
    val imageLoader: ImageLoader
) : Fetcher {

    override suspend fun fetch(): FetchResult? = when (val result = getAvatarAsset(data.userAssetId)) {
        is PublicAssetResult.Failure -> null
        is PublicAssetResult.Success -> {
            // Does coil cache this in memory? We can add our own cache if needed
            // imageLoader.memoryCache.set(MemoryCache.Key("assetKey"), MemoryCache.Value("result.asset.toBitmap()"))
            DrawableResult(
                BitmapDrawable(resources, result.asset.toBitmap()), false, DataSource.DISK
            )
        }
    }

    class Factory(
        private val getAvatarAssetUseCase: GetAvatarAssetUseCase,
        private val resources: Resources
    ) : Fetcher.Factory<UserAvatarAsset> {
        override fun create(data: UserAvatarAsset, options: Options, imageLoader: ImageLoader): Fetcher =
            AssetImageFetcher(data, getAvatarAssetUseCase, resources, imageLoader)
    }
}
