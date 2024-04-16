/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.util.ui

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
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
                        is PublicAssetResult.Failure ->
                            throw AssetImageException(retryPolicy(result.isRetryNeeded, result.coreFailure))

                        is PublicAssetResult.Success -> {
                            drawableResultWrapper.toFetchResult(result.assetPath)
                        }
                    }
                }

                is ImageAsset.PrivateAsset -> {
                    when (val result = getPrivateAsset(data.conversationId, data.messageId).await()) {
                        is MessageAssetResult.Failure ->
                            throw AssetImageException(retryPolicy(result.isRetryNeeded, result.coreFailure))

                        is MessageAssetResult.Success -> {
                            drawableResultWrapper.toFetchResult(result.decodedAssetPath)
                        }
                    }
                }
            }
        }
    }

    private fun retryPolicy(isRetryNeeded: Boolean, coreFailure: CoreFailure): AssetImageRetryPolicy = when {
        !isRetryNeeded -> AssetImageRetryPolicy.DO_NOT_RETRY
        coreFailure is NetworkFailure.NoNetworkConnection -> AssetImageRetryPolicy.RETRY_WHEN_CONNECTED
        else -> AssetImageRetryPolicy.EXPONENTIAL_RETRY_WHEN_CONNECTED
    }

    class Factory(
        private val getPublicAssetUseCase: GetAvatarAssetUseCase,
        private val getPrivateAssetUseCase: GetMessageAssetUseCase,
        private val deleteAssetUseCase: DeleteAssetUseCase,
        private val drawableResultWrapper: DrawableResultWrapper,
    ) : Fetcher.Factory<ImageAsset.Network> {
        override fun create(
            data: ImageAsset.Network,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = AssetImageFetcher(
            assetFetcherParameters = AssetFetcherParameters(data, options),
            getPublicAsset = getPublicAssetUseCase,
            getPrivateAsset = getPrivateAssetUseCase,
            deleteAsset = deleteAssetUseCase,
            drawableResultWrapper = drawableResultWrapper,
        )
    }
}

data class AssetFetcherParameters(
    val data: ImageAsset.Network,
    val options: Options
)

data class AssetImageException(val retryPolicy: AssetImageRetryPolicy) : Exception("Load asset image exception")

enum class AssetImageRetryPolicy {
    RETRY_WHEN_CONNECTED,
    EXPONENTIAL_RETRY_WHEN_CONNECTED,
    DO_NOT_RETRY
}
