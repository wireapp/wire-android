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

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.net.URI

object SupportUrlResolver {

    private const val SUPPORT_PATH = "support"

    @Volatile
    private var backendWebsiteUrl: String? = null

    fun setBackendWebsiteUrl(url: String?) {
        backendWebsiteUrl = url?.takeIf { it.isNotBlank() }
    }

    fun setBaseUrl(baseUrl: String?) {
        setBackendWebsiteUrl(baseUrl)
    }

    fun resolveUrl(url: String): String? {
        val trimmedUrl = url.trim()
        return if (trimmedUrl.isBlank()) {
            backendWebsiteUrl?.trimEnd('/')?.let { "$it/$SUPPORT_PATH" }
        } else {
            trimmedUrl.takeIf { it.isHttpUrl() }
        }
    }

    fun resolve(
        hardcodedUrl: String,
        page: SupportPage
    ): String = hardcodedUrl.takeIf { it.isNotBlank() } ?: buildDynamicUrl(page)

    fun resolve(
        resources: Resources,
        page: SupportPage
    ): String = resolve(resources.getString(page.hardcodedUrlRes), page)

    fun resolve(
        resources: Resources,
        @StringRes hardcodedUrlRes: Int,
        page: SupportPage
    ): String = resolve(resources.getString(hardcodedUrlRes), page)

    private fun buildDynamicUrl(page: SupportPage): String {
        val supportUrl = "${backendWebsiteUrl.orEmpty().trim().trimEnd('/')}/$SUPPORT_PATH"
        return if (page.path.isEmpty()) supportUrl else "$supportUrl/${page.path}"
    }

    private fun String.isHttpUrl(): Boolean =
        runCatching {
            val uri = URI(this)
            uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
}

@Composable
fun supportUrlResource(
    page: SupportPage
): String = SupportUrlResolver.resolve(stringResource(id = page.hardcodedUrlRes), page)

@Composable
fun supportUrlResource(
    @StringRes hardcodedUrlRes: Int
): String {
    val hardcodedUrl = stringResource(id = hardcodedUrlRes)
    return SupportPage.fromHardcodedUrlRes(hardcodedUrlRes)
        ?.let { SupportUrlResolver.resolve(hardcodedUrl, it) }
        ?: SupportUrlResolver.resolveUrl(hardcodedUrl).orEmpty()
}
