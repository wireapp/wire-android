package com.wire.android.util.ui

import android.content.res.Resources
import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult

internal class AssetImageFetcher(
    private val data: ImageAsset,
    private val getPublicAsset: GetAvatarAssetUseCase,
    private val getPrivateAsset: GetMessageAssetUseCase,
    private val resources: Resources,
    private val drawableResultWrapper: DrawableResultWrapper = DrawableResultWrapper(resources),
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return when (data) {
            is ImageAsset.UserAvatarAsset -> {
                when (val result = getPublicAsset(data.userAssetId)) {
                    is PublicAssetResult.Failure -> null
                    is PublicAssetResult.Success -> {
                        drawableResultWrapper.toFetchResult(result.assetPath)
                    }
                }
            }

            is ImageAsset.PrivateAsset -> {
                when (val result = getPrivateAsset(data.conversationId, data.messageId)) {
                    is MessageAssetResult.Failure -> null
                    is MessageAssetResult.Success -> {
                        drawableResultWrapper.toFetchResult(result.decodedAssetPath)
                    }
                }
            }
        }
    }

    class Factory(
        private val getPublicAssetUseCase: GetAvatarAssetUseCase,
        private val getPrivateAssetUseCase: GetMessageAssetUseCase,
        private val resources: Resources,
    ) : Fetcher.Factory<ImageAsset> {
        override fun create(data: ImageAsset, options: Options, imageLoader: ImageLoader): Fetcher =
            AssetImageFetcher(
                data = data,
                getPublicAsset = getPublicAssetUseCase,
                getPrivateAsset = getPrivateAssetUseCase,
                resources = resources,
            )
    }
}
