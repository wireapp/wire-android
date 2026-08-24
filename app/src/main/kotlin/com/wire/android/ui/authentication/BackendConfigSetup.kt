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

package com.wire.android.ui.authentication

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.wire.android.ui.WireActivity
import com.wire.kalium.logic.configuration.server.ServerConfig

fun ServerConfig.Links.isConfigured() = api.isNotBlank()

fun Context.openBackendConfig(input: String) {
    val sanitizedInput = input.trim()
    if (sanitizedInput.isEmpty()) return

    val deepLinkUri = if (sanitizedInput.startsWith(WIRE_ACCESS_DEEPLINK_PREFIX)) {
        Uri.parse(sanitizedInput)
    } else {
        Uri.parse("$WIRE_ACCESS_DEEPLINK_PREFIX${Uri.encode(sanitizedInput)}")
    }

    startActivity(
        Intent(this, WireActivity::class.java).apply {
            data = deepLinkUri
        }
    )
}

fun Context.openExternalCamera(): Boolean {
    return try {
        startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private const val BACKEND_CONFIG_QUERY_PARAMETER = "config"
private const val WIRE_ACCESS_DEEPLINK_PREFIX = "wire://access/?$BACKEND_CONFIG_QUERY_PARAMETER="
