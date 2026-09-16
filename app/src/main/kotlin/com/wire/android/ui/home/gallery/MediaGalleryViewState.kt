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

package com.wire.android.ui.home.gallery

import com.wire.android.model.ImageAsset

data class MediaGalleryViewState(
    val assetState: MediaGalleryAssetState = MediaGalleryAssetState.Loading,
    val screenTitle: String? = null,
    val menuItems: List<MediaGalleryMenuItem> = emptyList(),
    val viewerAccess: Boolean = false,
)

/**
 * Loading state of the image shown by the gallery. Modelled explicitly so that an asset we cannot
 * resolve (missing content URL, deleted local file, …) surfaces as an error with a retry action
 * instead of rendering an empty screen.
 */
sealed interface MediaGalleryAssetState {
    data object Loading : MediaGalleryAssetState
    data class Loaded(val image: MediaGalleryImage) : MediaGalleryAssetState
    data object Failure : MediaGalleryAssetState
}

sealed interface MediaGalleryImage {
    data class PrivateAsset(val asset: ImageAsset.PrivateAsset) : MediaGalleryImage
    data class LocalAsset(val path: String) : MediaGalleryImage
    data class UrlAsset(val url: String, val placeholder: String?, val contentHash: String?) : MediaGalleryImage
}
