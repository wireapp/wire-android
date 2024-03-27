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
package com.wire.android.ui.sharing

import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.util.ui.WireSessionImageLoader

sealed class ImportedMediaAsset(
    open val assetBundle: AssetBundle,
    open val assetSizeExceeded: Int?
) {
    class GenericAsset(
        override val assetBundle: AssetBundle,
        override val assetSizeExceeded: Int?,
    ) : ImportedMediaAsset(assetBundle, assetSizeExceeded)

    class Image(
        val width: Int,
        val height: Int,
        override val assetBundle: AssetBundle,
        override val assetSizeExceeded: Int?,
        val wireSessionImageLoader: WireSessionImageLoader
    ) : ImportedMediaAsset(assetBundle, assetSizeExceeded) {
        val localImageAsset = ImageAsset.LocalImageAsset(wireSessionImageLoader, assetBundle.dataPath, assetBundle.key)
    }
}
