/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import android.content.Context
import com.wire.android.ui.common.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FileSizeFormatter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Suppress("MagicNumber")
    fun formatSize(bytes: Long): String {
        context.resources.getString(R.string.size_unit_bytes)
        val units = arrayOf(
            context.resources.getString(R.string.size_unit_bytes),
            context.resources.getString(R.string.size_unit_kilobytes),
            context.resources.getString(R.string.size_unit_megabytes),
            context.resources.getString(R.string.size_unit_gigabytes),
            context.resources.getString(R.string.size_unit_terabytes)
        )
        var size = bytes.toDouble()
        var index = 0

        while (size >= 1024 && index < units.size - 1) {
            size /= 1024
            index++
        }
        return String.format("%.2f %s", size, units[index])
    }
}
