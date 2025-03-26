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
package com.wire.android.feature.cells.ui.model

import com.wire.android.feature.cells.R

enum class FileAction(val title: Int, val icon: Int, val isHighlighted: Boolean = false) {
    SAVE(R.string.save_file, R.drawable.ic_file_save),
    SHARE(R.string.share_file, R.drawable.ic_file_share),
    PUBLIC_LINK(R.string.public_link, R.drawable.ic_file_link),
    DELETE(R.string.delete_file, R.drawable.ic_file_delete, true),
}
