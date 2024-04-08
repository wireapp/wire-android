/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.authentication.welcome

import android.content.Context
import android.content.Intent
import com.wire.android.util.getMimeType
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import com.wire.kalium.logic.configuration.server.ServerConfig
import java.io.File

data class WelcomeScreenState(
    val links: ServerConfig.Links,
    val isThereActiveSession: Boolean = false,
    val maxAccountsReached: Boolean = false,
    val isLoggingEnabled: Boolean = false,
    val logFileLocation: String = "",
) {

    fun shareLogs(context: Context) {
        val dir = File(logFileLocation)
        val fileUris = context.getUrisOfFilesInDirectory(dir)
        val intent = context.multipleFileSharingIntent(fileUris)
        // The first log file is simply text, not compressed. Get its mime type separately
        // and set it as the mime type for the intent.
        intent.type = fileUris.firstOrNull()?.getMimeType(context) ?: "text/plain"
        // Get all other mime types and add them
        val mimeTypes = fileUris.drop(1).mapNotNull { it.getMimeType(context) }
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toSet().toTypedArray())
        context.startActivity(intent)
    }
}
