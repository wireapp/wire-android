package com.wire.android.di

import android.content.Context
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.ViewModelScoped

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
        getAvatarAssetUseCase: GetAvatarAssetUseCase
    ): WireSessionImageLoader.Factory = WireSessionImageLoader.Factory(context, getAvatarAssetUseCase)

    // For better performance/caching. We shouldn't create many of these ImageLoaders.
    @Provides
    fun provideWireImageLoader(imageLoaderFactory: WireSessionImageLoader.Factory) = imageLoaderFactory.newImageLoader()

}
