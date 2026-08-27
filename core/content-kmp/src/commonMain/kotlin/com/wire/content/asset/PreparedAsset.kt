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

package com.wire.content.asset

import com.wire.kalium.logic.data.asset.AttachmentType
import okio.Path
import kotlin.math.roundToInt

/**
 * An asset that has been copied into app-private storage and is ready for policy checks or upload.
 *
 * Platform references such as Android content URIs deliberately do not belong in this model.
 */
data class PreparedAsset(
    val key: String,
    val mimeType: String,
    val dataPath: Path,
    val dataSize: Long,
    val fileName: String,
    val assetType: AttachmentType,
    val audioWavesMask: List<Int>? = null,
) {
    val extensionWithSize: String
        get() {
            val assetExtension = fileName.substringAfterLast('.')
            val oneMegabyte = BYTES_PER_KILOBYTE * BYTES_PER_KILOBYTE
            return when {
                dataSize < BYTES_PER_KILOBYTE -> "${assetExtension.uppercase()} ($dataSize B)"
                dataSize in BYTES_PER_KILOBYTE..oneMegabyte ->
                    "${assetExtension.uppercase()} (${dataSize / BYTES_PER_KILOBYTE} KB)"
                else ->
                    "${assetExtension.uppercase()} " +
                        "(${((dataSize / oneMegabyte) * SIZE_DECIMAL_SCALE).roundToInt() / SIZE_DECIMAL_SCALE} MB)"
            }
        }

    val assetName: String
        get() = fileName.substringBefore('.')

    private companion object {
        const val BYTES_PER_KILOBYTE = 1024L
        const val SIZE_DECIMAL_SCALE = 100.0
    }
}
