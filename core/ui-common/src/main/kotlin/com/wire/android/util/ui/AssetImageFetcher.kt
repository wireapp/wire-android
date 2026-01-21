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

import coil3.Extras
import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import com.wire.android.model.ImageAsset
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class AssetImageFetcher(
    private val assetFetcherParameters: AssetFetcherParameters,
    private val getPublicAsset: GetAvatarAssetUseCase,
    private val getPrivateAsset: GetMessageAssetUseCase,
    private val deleteAsset: DeleteAssetUseCase,
    private val drawableResultWrapper: DrawableResultWrapper
) : Fetcher {

    companion object {
        val OPTION_PARAMETER_RETRY_KEY = Extras.Key<Int>(default = DEFAULT_RETRY_ATTEMPT)
        private const val RETRY_ATTEMPT_TO_DELETE_ASSET = 1
        private const val DEFAULT_RETRY_ATTEMPT = 0
    }

    override suspend fun fetch(): FetchResult = MutexMap.withLock(assetFetcherParameters.data.uniqueKey, ::fetchJob)

    private suspend fun fetchJob(): FetchResult {
        with(assetFetcherParameters) {
            return when (data) {
                is ImageAsset.UserAvatarAsset -> {
                    val retryHash = options.extras[OPTION_PARAMETER_RETRY_KEY]
                    if (retryHash != null && retryHash >= RETRY_ATTEMPT_TO_DELETE_ASSET) {
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
    ) : Fetcher.Factory<ImageAsset.Remote> {
        override fun create(
            data: ImageAsset.Remote,
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
    val data: ImageAsset.Remote,
    val options: Options
)

data class AssetImageException(val retryPolicy: AssetImageRetryPolicy) : Exception("Load asset image exception")

enum class AssetImageRetryPolicy {
    RETRY_WHEN_CONNECTED,
    EXPONENTIAL_RETRY_WHEN_CONNECTED,
    DO_NOT_RETRY
}

/**
 * Creates a mutex associated with a key and locks it so that there's only one execution going for a given key at a given time.
 * When the lock for the given key is executed for the first time, it will create a new entry in the map and lock the mutex.
 * When another lock is executed for the same key while it's locked, it will increase the count and lock the mutex so that it'll wait
 * and execute after the first execution unlocks the mutex. The count is there to keep the mutex in the map as long as it's needed.
 * After the last unlock, the mutex is removed from the map.
 */
private object MutexMap {
    private val assetMutex = ConcurrentHashMap<String, Pair<Int, Mutex>>()

    suspend fun <T> withLock(key: String, action: suspend () -> T): T =
        increaseCountAndGetMutex(key).let { (_, mutex) ->
            mutex.withLock {
                action().also {
                    decreaseCountAndRemoveMutexIfNeeded(key)
                }
            }
        }

    private fun increaseCountAndGetMutex(key: String): Pair<Int, Mutex> =
        assetMutex.compute(key) { _, value ->
            ((value ?: (0 to Mutex()))).let { (count, mutex) ->
                count + 1 to mutex
            }
        }!!

    private fun decreaseCountAndRemoveMutexIfNeeded(key: String) {
        assetMutex.compute(key) { _, value ->
            value?.let { (count, mutex) ->
                if (count <= 1) {
                    null
                } else {
                    count - 1 to mutex
                }
            }
        }
    }
}
