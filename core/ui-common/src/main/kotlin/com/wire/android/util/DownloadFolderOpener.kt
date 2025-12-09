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

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.wire.android.ui.common.R

fun openDownloadFolder(
    context: Context,
) {
    val errorToastMessage = context.resources.getString(R.string.label_no_application_found_open_downloads_folder)
    try {
        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, errorToastMessage, Toast.LENGTH_SHORT).show()
    }
}
