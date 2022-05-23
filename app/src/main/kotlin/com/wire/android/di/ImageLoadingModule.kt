package com.wire.android.di

import android.content.Context
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Module that holds everything necessary to load images.
 * It's installed in [ViewModelComponent] as it is something that depends on the currently active user session
 */
@Module
@InstallIn(ViewModelComponent::class)
class ImageLoadingModule {

    @Provides
    fun provideImageLoaderFactory(
        @ApplicationContext context: Context,
        getAvatarAsset: GetAvatarAssetUseCase,
        getMessageAsset: GetMessageAssetUseCase,
        dispatcherProvider: DispatcherProvider,
    ): WireSessionImageLoader.Factory = WireSessionImageLoader.Factory(
        context = context,
        getAvatarAsset = getAvatarAsset,
        getPrivateAsset = getMessageAsset,
        dispatcherProvider = dispatcherProvider
    )

    // For better performance/caching. We shouldn't create many of these ImageLoaders.
    @Provides
    fun provideWireImageLoader(imageLoaderFactory: WireSessionImageLoader.Factory) = imageLoaderFactory.newImageLoader()

}
