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

package com.wire.android.di

import android.content.Context
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.network.NetworkStateObserver
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
        deleteAsset: DeleteAssetUseCase,
        getMessageAsset: GetMessageAssetUseCase,
        networkStateObserver: NetworkStateObserver,
    ): WireSessionImageLoader.Factory = WireSessionImageLoader.Factory(
        context = context,
        getAvatarAsset = getAvatarAsset,
        deleteAsset = deleteAsset,
        networkStateObserver = networkStateObserver,
        getPrivateAsset = getMessageAsset
    )

    // For better performance/caching. We shouldn't create many of these ImageLoaders.
    @Provides
    fun provideWireImageLoader(imageLoaderFactory: WireSessionImageLoader.Factory) = imageLoaderFactory.newImageLoader()
}
