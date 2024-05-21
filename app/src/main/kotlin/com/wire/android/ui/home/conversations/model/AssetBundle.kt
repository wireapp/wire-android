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
import androidx.compose.runtime.Stable
import com.wire.kalium.logic.data.asset.AttachmentType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okio.Path
import okio.Path.Companion.toPath
import kotlin.math.roundToInt

/**
 * Represents a set of metadata information of an asset message
 */
@Serializable
data class AssetBundle(
    val key: String,
    val mimeType: String,
    @Serializable(with = PathAsStringSerializer::class)
    val dataPath: Path,
    val dataSize: Long,
    val fileName: String,
    val assetType: AttachmentType
) {

    @Stable
    val extensionWithSize: String
        get() {
            val assetExtension = fileName.split(".").last()
            val oneKB = 1024L
            val oneMB = oneKB * oneKB
            return when {
                dataSize < oneKB -> "${assetExtension.uppercase()} ($dataSize B)"
                dataSize in oneKB..oneMB -> "${assetExtension.uppercase()} (${dataSize / oneKB} KB)"
                else -> "${assetExtension.uppercase()} (${((dataSize / oneMB) * 100.0).roundToInt() / 100.0} MB)" // 2 decimals round off
            }
        }

    val assetName: String
        get() = fileName.split(".").first()
}

/**
 * @param uri Uri of the asset
 * @param saveToDeviceIfInvalid if true then the asset will be copied to the public "media" directory if it's invalid (e.g. too large)
 */
data class UriAsset(
    val uri: Uri,
    val saveToDeviceIfInvalid: Boolean = false
)

private object PathAsStringSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Path {
        return decoder.decodeString().toPath()
    }
}
