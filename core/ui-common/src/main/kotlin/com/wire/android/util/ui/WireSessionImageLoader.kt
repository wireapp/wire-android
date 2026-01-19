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

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wire.android.model.ImageAsset
import com.wire.android.util.ui.AssetImageFetcher.Companion.OPTION_PARAMETER_RETRY_KEY
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.util.ExponentialDurationHelper
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * An ImageLoader that is able to load AssetIds supplied by Kalium.
 * As it uses Kalium's [GetAvatarAssetUseCase], a loader created for one session may be unable to load images from another session.
 * It wraps Coil, so it becomes easier to refactor in the future if we ever switch from Coil to something else.
 */
@Stable
class WireSessionImageLoader(
    private val coilImageLoader: ImageLoader,
    private val networkStateObserver: NetworkStateObserver
) {
    private companion object {
        const val RETRY_INCREMENT_ATTEMPT_PER_STEP = 1
        val MIN_RETRY_DELAY = 1.seconds
        val MAX_RETRY_DELAY = 10.minutes
    }

    /**
     * Attempts to paint an Image using [asset], falling back to [fallbackData] if [asset] is null.
     * Just like [rememberAsyncImagePainter], [fallbackData] can be anything that [Coil] accepts.
     * Currently, Coil does not have a friendly API to retry a failing image request, so we have to do it ourselves.
     * adding retry_hash is a workaround to force Coil to retry the request.
     * see https://github.com/coil-kt/coil/issues/884
     */
    @Composable
    fun paint(
        asset: ImageAsset.Remote?,
        fallbackData: Any? = null,
        withCrossfadeAnimation: Boolean = false,
    ): Painter {
        var retryHash by remember { mutableStateOf(0) }
        val exponentialDurationHelper = remember { ExponentialDurationHelper(MIN_RETRY_DELAY, MAX_RETRY_DELAY) }
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .memoryCacheKey(asset?.uniqueKey)
                .data(asset ?: fallbackData)
                .apply {
                    extras[OPTION_PARAMETER_RETRY_KEY] = retryHash
                }
                .crossfade(withCrossfadeAnimation)
                .build(),
            error = (fallbackData as? Int)?.let { painterResource(id = it) },
            imageLoader = coilImageLoader
        )

        LaunchedEffect(painter.state) {
            if (painter.state.value is AsyncImagePainter.State.Error) {
                val retryPolicy =
                    ((painter.state.value as AsyncImagePainter.State.Error).result.throwable as? AssetImageException)?.retryPolicy
                        ?: AssetImageRetryPolicy.DO_NOT_RETRY

                if (retryPolicy == AssetImageRetryPolicy.EXPONENTIAL_RETRY_WHEN_CONNECTED) {
                    delay(exponentialDurationHelper.next())
                }

                if (retryPolicy != AssetImageRetryPolicy.DO_NOT_RETRY) {
                    networkStateObserver.observeNetworkState().firstOrNull { it == NetworkState.ConnectedWithInternet }
                    retryHash += 1
                }
            } else {
                exponentialDurationHelper.reset()
            }
        }
        return painter
    }

    class Factory(
        val context: Context,
        private val getAvatarAsset: GetAvatarAssetUseCase,
        private val deleteAsset: DeleteAssetUseCase,
        private val getPrivateAsset: GetMessageAssetUseCase,
        private val networkStateObserver: NetworkStateObserver,
    ) {
        fun newImageLoader(): WireSessionImageLoader =
            WireSessionImageLoader(
                ImageLoader.Builder(context)
                    .components {
                        add(
                            AssetImageFetcher.Factory(
                                getPublicAssetUseCase = getAvatarAsset,
                                getPrivateAssetUseCase = getPrivateAsset,
                                deleteAssetUseCase = deleteAsset,
                                drawableResultWrapper = DrawableResultWrapper(),
                            )
                        )
                        if (SDK_INT >= VERSION_CODES.P) {
                            add(AnimatedImageDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }.build(),
                networkStateObserver
            )
    }
}
