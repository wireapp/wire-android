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
package com.wire.android.feature.cells.ui.movetofolder

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MoveToFolderNavArgs(
    val currentPath: String,
    val nodeToMovePath: String,
    val uuid: String,
    val breadcrumbs: Array<String> = emptyArray()
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MoveToFolderNavArgs

        if (currentPath != other.currentPath) return false
        if (nodeToMovePath != other.nodeToMovePath) return false
        if (uuid != other.uuid) return false
        if (!breadcrumbs.contentEquals(other.breadcrumbs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentPath.hashCode()
        result = 31 * result + nodeToMovePath.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + breadcrumbs.contentHashCode()
        return result
    }
}
