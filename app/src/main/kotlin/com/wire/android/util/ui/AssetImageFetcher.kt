package com.wire.android.util.ui

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

     companion object {
        const val OPTION_PARAMETER_RETRY_KEY = "retry_attempt"
        private const val RETRY_ATTEMPT_TO_DELETE_ASSET = 1
        private const val DEFAULT_RETRY_ATTEMPT = 0
    }

    override suspend fun fetch(): FetchResult? {
        with(assetFetcherParameters) {
            return when (data) {
                is ImageAsset.UserAvatarAsset -> {
                    val retryHash = options.parameters.value(OPTION_PARAMETER_RETRY_KEY) ?: DEFAULT_RETRY_ATTEMPT

                    if (retryHash >= RETRY_ATTEMPT_TO_DELETE_ASSET) {
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

