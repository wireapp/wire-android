/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.di.metro

import androidx.lifecycle.ViewModel
import com.wire.android.model.ImageAssetViewModelFactory
import com.wire.android.model.ImageAssetViewModelGraph
import com.wire.android.ui.calling.CallingViewModelFactory
import com.wire.android.ui.calling.CallingViewModelGraph
import com.wire.android.ui.home.settings.SettingsViewModelFactory
import com.wire.android.ui.home.settings.SettingsViewModelGraph
import com.wire.android.util.ui.WireSessionImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider

/**
 * Android-only bridge that exposes the image asset graph while ui-common is being decoupled from Hilt.
 */
@HiltViewModel
class ImageAssetViewModelGraphBridgeViewModel @Inject constructor(
    imageLoader: Provider<WireSessionImageLoader>,
    private val callingViewModelFactoryProvider: Provider<CallingViewModelFactory>,
    private val settingsViewModelFactoryProvider: Provider<SettingsViewModelFactory>,
) : ViewModel(), ImageAssetViewModelGraph, CallingViewModelGraph, SettingsViewModelGraph {
    override val imageAssetViewModelFactory: ImageAssetViewModelFactory =
        ImageAssetViewModelFactory(imageLoader = imageLoader::get)

    override val callingViewModelFactory: CallingViewModelFactory
        get() = callingViewModelFactoryProvider.get()

    override val settingsViewModelFactory: SettingsViewModelFactory
        get() = settingsViewModelFactoryProvider.get()
}
