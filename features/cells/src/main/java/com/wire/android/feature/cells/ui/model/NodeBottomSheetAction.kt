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

enum class NodeBottomSheetAction(
    val title: Int,
    val icon: Int,
    val isHighlighted: Boolean = false
) {
    SHARE(R.string.share_label, R.drawable.ic_share),
    PUBLIC_LINK(R.string.public_link, com.wire.android.ui.common.R.drawable.ic_link),
    ADD_REMOVE_TAGS(R.string.add_remove_tags_label, R.drawable.ic_tags),
    MOVE(R.string.move_label, R.drawable.ic_folder),
    RENAME(R.string.rename_label, R.drawable.ic_rename),
    DOWNLOAD(R.string.download_label, R.drawable.ic_save),
    RESTORE(R.string.restore_label, R.drawable.ic_restore),
    DELETE(R.string.delete_label, com.wire.android.ui.common.R.drawable.ic_delete, true),
    DELETE_PERMANENTLY(R.string.delete_permanently, com.wire.android.ui.common.R.drawable.ic_delete, true),
    VERSION_HISTORY(R.string.see_version_history_bottom_sheet, com.wire.android.ui.common.R.drawable.ic_version_history)
}
