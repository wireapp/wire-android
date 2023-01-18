package com.wire.android.util.ui

import android.content.res.Resources
import android.util.Log
import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult

internal class AssetImageFetcher(
    private val assetFetcherParameters: AssetFetcherParameters,
    private val getPublicAsset: GetAvatarAssetUseCase,
    private val getPrivateAsset: GetMessageAssetUseCase,
    private val deleteAsset: DeleteAssetUseCase,
    private val drawableResultWrapper: DrawableResultWrapper
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        with(assetFetcherParameters) {
            return when (data) {
                is ImageAsset.UserAvatarAsset -> {
                    val retryHash = options.parameters.value("retry_hash") ?: 0

                    if (retryHash >= 1) {
                        deleteAsset(data.userAssetId)
                    }

                    when (val result = getPublicAsset(data.userAssetId)) {
                        is PublicAssetResult.Failure -> null
                        is PublicAssetResult.Success -> {
                            drawableResultWrapper.toFetchResult(result.assetPath)
                        }
                    }
                }

                is ImageAsset.PrivateAsset -> {
                    when (val result = getPrivateAsset(data.conversationId, data.messageId).await()) {
                        is MessageAssetResult.Failure -> null
                        is MessageAssetResult.Success -> {
                            drawableResultWrapper.toFetchResult(result.decodedAssetPath)
                        }
                    }
                }
            }
        }
    }

    class Factory(
        private val getPublicAssetUseCase: GetAvatarAssetUseCase,
        private val getPrivateAssetUseCase: GetMessageAssetUseCase,
        private val deleteAssetUseCase: DeleteAssetUseCase,
        private val drawableResultWrapper: DrawableResultWrapper
    ) : Fetcher.Factory<ImageAsset> {
        override fun create(
            data: ImageAsset,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AssetImageFetcher(
            assetFetcherParameters = AssetFetcherParameters(data, options),
            getPublicAsset = getPublicAssetUseCase,
            getPrivateAsset = getPrivateAssetUseCase,
            deleteAsset = deleteAssetUseCase,
            drawableResultWrapper = drawableResultWrapper
        )
    }

}

data class AssetFetcherParameters(
    val data: ImageAsset,
    val options: Options
)


