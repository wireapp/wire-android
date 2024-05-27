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
    private const val BYTES_IN_KILOBYTE = 1024L
    private const val BYTES_IN_MEGABYTE = BYTES_IN_KILOBYTE * 1024
    private const val BYTES_IN_GIGABYTE = BYTES_IN_MEGABYTE * 1024

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

    fun formatSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < BYTES_IN_KILOBYTE -> "$sizeInBytes B"
            sizeInBytes < BYTES_IN_MEGABYTE -> String.format("%.2f KB", sizeInBytes.toDouble() / BYTES_IN_KILOBYTE)
            sizeInBytes < BYTES_IN_GIGABYTE -> String.format("%.2f MB", sizeInBytes.toDouble() / BYTES_IN_MEGABYTE)
            else -> String.format("%.2f GB", sizeInBytes.toDouble() / BYTES_IN_GIGABYTE)
        }
    }
}
