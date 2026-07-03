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
package com.wire.android.util

import java.net.URI

object SupportUrlResolver {

    private const val SUPPORT_PATH = "support"

    @Volatile
    private var backendWebsiteUrl: String? = null

    fun setBackendWebsiteUrl(url: String?) {
        backendWebsiteUrl = url?.takeIf { it.isNotBlank() }
    }

    fun resolveUrl(url: String): String? {
        val trimmedUrl = url.trim()
        return if (trimmedUrl.isBlank()) {
            backendWebsiteUrl?.trimEnd('/')?.let { "$it/$SUPPORT_PATH" }
        } else {
            trimmedUrl.takeIf { it.isHttpUrl() }
        }
    }

    private fun String.isHttpUrl(): Boolean =
        runCatching {
            val uri = URI(this)
            uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
}
