package com.wire.android.util.ui

import android.content.Context
import android.content.res.Resources
import androidx.core.net.toFile
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.wire.android.model.ImageAsset
import com.wire.android.util.toBitmap
import com.wire.android.util.toDrawable
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult

internal class AssetImageFetcher(
    private val data: ImageAsset,
    private val getPublicAsset: GetAvatarAssetUseCase,
    private val getPrivateAsset: GetMessageAssetUseCase,
    private val context: Context,
    private val drawableResultWrapper: DrawableResultWrapper = DrawableResultWrapper(context.resources),
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
                when (val result = getPrivateAsset(data.conversationId, data.messageId).await()) {
                    is MessageAssetResult.Failure -> null
                    is MessageAssetResult.Success -> {
                        drawableResultWrapper.toFetchResult(result.decodedAssetPath)
                    }
                }
            }

            is ImageAsset.LocalImageAsset -> {
                data.dataUri.toDrawable(context)?.let {
                    DrawableResult(
                        drawable = it,
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                }
            }
        }
    }

    class Factory(
        private val getPublicAssetUseCase: GetAvatarAssetUseCase,
        private val getPrivateAssetUseCase: GetMessageAssetUseCase,
        private val context: Context,
    ) : Fetcher.Factory<ImageAsset> {
        override fun create(data: ImageAsset, options: Options, imageLoader: ImageLoader): Fetcher =
            AssetImageFetcher(
                data = data,
                getPublicAsset = getPublicAssetUseCase,
                getPrivateAsset = getPrivateAssetUseCase,
                context = context,
            )
    }
}
