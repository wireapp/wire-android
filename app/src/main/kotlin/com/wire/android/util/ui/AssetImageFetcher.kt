package com.wire.android.util.ui

import android.content.res.Resources
import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult

internal class AssetImageFetcher(
    val data: ImageAsset,
    val getPublicAsset: GetAvatarAssetUseCase,
    val getPrivateAsset: GetMessageAssetUseCase,
    val resources: Resources,
    val drawableResultWrapper: DrawableResultWrapper = DrawableResultWrapper(resources),
    val kaliumFileSystem: KaliumFileSystem
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return when (data) {
            is ImageAsset.UserAvatarAsset -> {
                when (val result = getPublicAsset(data.userAssetId)) {
                    is PublicAssetResult.Failure -> null
                    is PublicAssetResult.Success -> {
                        // Does coil cache this in memory? We can add our own cache if needed
                        // imageLoader.memoryCache.set(MemoryCache.Key("assetKey"), MemoryCache.Value("result.asset.toBitmap()"))
                        val imageSource = kaliumFileSystem.source(result.assetPath)
                        drawableResultWrapper.toFetchResult(imageSource)
                    }
                }
            }
            is ImageAsset.PrivateAsset -> {
                when (val result = getPrivateAsset(data.conversationId, data.messageId)) {
                    is MessageAssetResult.Failure -> null
                    is MessageAssetResult.Success -> {
                        // Does coil cache this in memory? We can add our own cache if needed
                        // imageLoader.memoryCache.set(MemoryCache.Key("assetKey"), MemoryCache.Value("result.asset.toBitmap()"))
                        val imageSource = kaliumFileSystem.source(result.decodedAssetPath)
                        drawableResultWrapper.toFetchResult(imageSource)
                    }
                }
            }
        }
    }

    class Factory(
        private val getPublicAssetUseCase: GetAvatarAssetUseCase,
        private val getPrivateAssetUseCase: GetMessageAssetUseCase,
        private val resources: Resources,
        private val kaliumFileSystem: KaliumFileSystem
    ) : Fetcher.Factory<ImageAsset> {
        override fun create(data: ImageAsset, options: Options, imageLoader: ImageLoader): Fetcher =
            AssetImageFetcher(
                data = data,
                getPublicAsset = getPublicAssetUseCase,
                getPrivateAsset = getPrivateAssetUseCase,
                resources = resources,
                kaliumFileSystem = kaliumFileSystem
            )
    }
}
