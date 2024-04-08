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

package com.wire.android.ui.home.conversations.model

import android.net.Uri
import com.wire.kalium.logic.data.asset.AttachmentType
import okio.Path

/**
 * Represents a set of metadata information of an asset message
 */
data class AssetBundle(
    val key: String,
    val mimeType: String,
    val dataPath: Path,
    val dataSize: Long,
    val fileName: String,
    val assetType: AttachmentType
)

/**
 * @param uri Uri of the asset
 * @param saveToDeviceIfInvalid if true then the asset will be copied to the public "media" directory if it's invalid (e.g. too large)
 */
data class UriAsset(
    val uri: Uri,
    val saveToDeviceIfInvalid: Boolean = false
)
