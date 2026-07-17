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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.wire.android.AppJsonStyledLogger
import com.wire.android.ui.common.R
import com.wire.kalium.logger.KaliumLogLevel

object CustomTabsHelper {

    private const val SUPPORT_PATH = "support"

    @Volatile
    private var backendWebsiteUrl: String? = null

    fun setBackendWebsiteUrl(url: String?) {
        backendWebsiteUrl = url?.takeIf { it.isNotBlank() }
    }

    fun launchUrl(context: Context, url: String) {
        val resolvedUrl = resolveUrl(url) ?: return
        launchUri(context, Uri.parse(resolvedUrl))
    }

    @JvmStatic
    fun launchUri(context: Context, uri: Uri) {
        try {
            val customTabsIntent = buildCustomTabIntent(context)
            customTabsIntent.launchUrl(context, uri)
        } catch (exception: ActivityNotFoundException) {
            AppJsonStyledLogger.log(
                level = KaliumLogLevel.ERROR,
                leadingMessage = "CustomTabsHelper",
                jsonStringKeyValues = mapOf("targetURI" to uri),
                error = exception
            )
        }
    }

    fun resolveUrl(url: String): String? =
        url.ifBlank {
            backendWebsiteUrl?.trimEnd('/')?.let { "$it/$SUPPORT_PATH" }
        }

    fun buildCustomTabIntent(context: Context): CustomTabsIntent {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setCloseButtonIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_close))
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setShowTitle(true)

        return customTabsIntent.build().apply {
            intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
        }
    }
}
