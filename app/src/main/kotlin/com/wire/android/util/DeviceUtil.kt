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

object DeviceUtil {
    private const val BYTES_IN_KILOBYTE = 1024
    private const val BYTES_IN_MEGABYTE = BYTES_IN_KILOBYTE * 1024
    private const val BYTES_IN_GIGABYTE = BYTES_IN_MEGABYTE * 1024
    private const val DIGITS_GROUP_SIZE = 3  // Number of digits between commas in formatted size.

    fun getAvailableInternalMemorySize(): String = try {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        formatSize(availableBlocks * blockSize)
    } catch (e: IllegalArgumentException) {
        ""
    }

    fun getTotalInternalMemorySize(): String = try {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        formatSize(totalBlocks * blockSize)
    } catch (e: IllegalArgumentException) {
        ""
    }

    private fun formatSize(sizeInBytes: Long): String {
        var size = sizeInBytes
        var suffix: String? = null
        when {
            size >= BYTES_IN_GIGABYTE -> {
                suffix = "GB"
                size /= BYTES_IN_GIGABYTE
            }

            size >= BYTES_IN_MEGABYTE -> {
                suffix = "MB"
                size /= BYTES_IN_MEGABYTE
            }

            size >= BYTES_IN_KILOBYTE -> {
                suffix = "KB"
                size /= BYTES_IN_KILOBYTE
            }
        }
        val resultBuffer = StringBuilder(size.toString())
        var commaOffset = resultBuffer.length - DIGITS_GROUP_SIZE
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',')
            commaOffset -= DIGITS_GROUP_SIZE
        }
        suffix?.let { resultBuffer.append(it) }
        return resultBuffer.toString()
    }
}
