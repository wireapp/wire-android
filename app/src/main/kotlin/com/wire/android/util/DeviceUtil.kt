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
package com.wire.android.util

import android.os.Environment
import android.os.StatFs

class DeviceUtil {
    companion object {
        fun getAvailableInternalMemorySize(): String {
            return try {
                val path = Environment.getDataDirectory()
                val stat = StatFs(path.path)
                val blockSize = stat.blockSizeLong
                val availableBlocks = stat.availableBlocksLong
                formatSize(availableBlocks * blockSize)
            } catch (e: IllegalArgumentException) {
                ""
            }
        }

        fun getTotalInternalMemorySize(): String {
            return try {
                val path = Environment.getDataDirectory()
                val stat = StatFs(path.path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                formatSize(totalBlocks * blockSize)
            } catch (e: IllegalArgumentException) {
                ""
            }
        }

        private fun formatSize(sizeInBytes: Long): String {
            var size = sizeInBytes
            var suffix: String? = null
            if (size >= 1024) {
                suffix = "KB"
                size /= 1024
                if (size >= 1024) {
                    suffix = "MB"
                    size /= 1024
                    if (size >= 1024) {
                        suffix = "GB"
                        size /= 1024
                    }
                }
            }
            val resultBuffer = StringBuilder(size.toString())
            var commaOffset = resultBuffer.length - 3
            while (commaOffset > 0) {
                resultBuffer.insert(commaOffset, ',')
                commaOffset -= 3
            }
            if (suffix != null) resultBuffer.append(suffix)
            return resultBuffer.toString()
        }
    }
}
