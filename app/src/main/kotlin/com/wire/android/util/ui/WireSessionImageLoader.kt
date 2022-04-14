package com.wire.android.util.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import coil.Coil
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.wire.android.model.UserAvatarAsset
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase

/**
 * An ImageLoader that is able to load AssetIds supplied by Kalium.
 * As it uses Kalium's [GetAvatarAssetUseCase], a loader created for one session may be unable to load images from another session.
 * It wraps Coil, so it becomes easier to refactor in the future if we ever switch from Coil to something else.
 */
class WireSessionImageLoader(private val coilImageLoader: ImageLoader) {

    /**
     * Attempts to paint an Image using [asset], falling back to [fallbackData] if [asset] is null.
     * Just like [rememberAsyncImagePainter], [fallbackData] can be anything that [Coil] accepts.
     */
    @Composable
    fun paint(
        asset: UserAvatarAsset?,
        fallbackData: Any? = null
    ): Painter = rememberAsyncImagePainter(asset ?: fallbackData, imageLoader = coilImageLoader)

    class Factory(
        context: Context,
        private val getAvatarAssetUseCase: GetAvatarAssetUseCase
    ) {
        private val defaultImageLoader = Coil.imageLoader(context)
        private val resources = context.resources

        fun newImageLoader(): WireSessionImageLoader = WireSessionImageLoader(
            defaultImageLoader.newBuilder()
                .components {
                    add(AssetImageFetcher.Factory(getAvatarAssetUseCase, resources))
                }.build()
        )
    }
}
