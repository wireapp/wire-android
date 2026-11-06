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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.R

enum class CellError(val message: Int) {
    FILE_NOT_SUPPORTED(R.string.file_not_supported),
    NO_APP_FOUND(R.string.no_app_found),
    OTHER_ERROR(R.string.action_failed),
    DOWNLOAD_FAILED(R.string.action_failed),
    ITEM_NO_LONGER_AVAILABLE(R.string.cells_item_no_longer_available),
    NO_SPACE_LEFT(R.string.no_space_left_error),
    UPLOAD_PREPARATION_FAILED(R.string.cells_upload_preparation_failed),
    UPLOAD_PREPARATION_PARTIALLY_FAILED(R.string.cells_upload_preparation_partially_failed),
}
