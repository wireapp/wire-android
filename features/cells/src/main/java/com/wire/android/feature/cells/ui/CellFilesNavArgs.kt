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
package com.wire.android.feature.cells.ui

data class CellFilesNavArgs(
    val conversationId: String? = null,
    val screenTitle: String? = null,
    val isRecycleBin: Boolean? = false,
    val breadcrumbs: Array<String>? = null,
    val parentFolderUuid: String? = null,
    val isSearchByDefaultActive: Boolean? = false,
) {

    override fun hashCode(): Int {
        var result = isRecycleBin?.hashCode() ?: 0
        result = 31 * result + (conversationId?.hashCode() ?: 0)
        result = 31 * result + (screenTitle?.hashCode() ?: 0)
        result = 31 * result + breadcrumbs.contentHashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CellFilesNavArgs

        if (isRecycleBin != other.isRecycleBin) return false
        if (conversationId != other.conversationId) return false
        if (screenTitle != other.screenTitle) return false
        if (breadcrumbs != null) {
            if (other.breadcrumbs == null) return false
            if (!breadcrumbs.contentEquals(other.breadcrumbs)) return false
        } else if (other.breadcrumbs != null) return false

        return true
    }
}
