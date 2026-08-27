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
import android.os.Parcel
import com.wire.content.asset.PreparedAsset
import com.wire.content.external.ExternalContentImportRequest
import com.wire.content.external.ExternalContentReference
import com.wire.kalium.logic.data.asset.AttachmentType
import kotlinx.parcelize.Parceler
import okio.Path.Companion.toPath

typealias AssetBundle = PreparedAsset
typealias UriAsset = ExternalContentImportRequest

/**
 * @param uri Uri of the asset
 * @param saveToDeviceIfInvalid if true then the asset will be copied to the public "media" directory if it's invalid (e.g. too large)
 */
@Suppress("FunctionName")
fun UriAsset(
    uri: Uri,
    saveToDeviceIfInvalid: Boolean = false,
    mimeType: String? = null,
    audioWavesMask: List<Int>? = null,
): ExternalContentImportRequest = ExternalContentImportRequest(
    reference = ExternalContentReference(uri.toString()),
    saveToDeviceIfInvalid = saveToDeviceIfInvalid,
    mimeType = mimeType,
    audioWavesMask = audioWavesMask,
)

val ExternalContentImportRequest.uri: Uri
    get() = Uri.parse(reference.token)

object PreparedAssetParceler : Parceler<PreparedAsset> {
    override fun create(parcel: Parcel) = PreparedAsset(
        key = parcel.readString().orEmpty(),
        mimeType = parcel.readString().orEmpty(),
        dataPath = parcel.readString().orEmpty().toPath(),
        dataSize = parcel.readLong(),
        fileName = parcel.readString().orEmpty(),
        assetType = AttachmentType.valueOf(parcel.readString().orEmpty()),
        audioWavesMask = parcel.createIntArray()?.toList(),
    )

    override fun PreparedAsset.write(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        parcel.writeString(mimeType)
        parcel.writeString(dataPath.toString())
        parcel.writeLong(dataSize)
        parcel.writeString(fileName)
        parcel.writeString(assetType.name)
        parcel.writeIntArray(audioWavesMask?.toIntArray())
    }
}
