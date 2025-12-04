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
package com.wire.android.feature.cells.ui.edit

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_SYSTEM
import javax.inject.Inject

class OnlineEditor @Inject constructor(
    private val context: Context,
) {

    fun open(url: String) {

        val uri = Uri.parse(url)

        if (isCustomTabsSupported()) {
            runCatching {
                openCustomTab(uri)
            }.onFailure {
                openInBrowser(uri)
            }
        } else {
            openInBrowser(uri)
        }
    }

    private fun openCustomTab(uri: Uri) {

        val customTabsIntent = CustomTabsIntent.Builder()
            .setCloseButtonIcon(BitmapFactory.decodeResource(context.resources, com.wire.android.ui.common.R.drawable.ic_close))
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setDownloadButtonEnabled(false)
            .setBookmarksButtonEnabled(false)
            .setColorScheme(getColorScheme())
            .setShowTitle(true)
            .build().apply {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
            }

        customTabsIntent.launchUrl(context, uri)
    }

    private fun openInBrowser(uri: Uri) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun getColorScheme() = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_NO -> COLOR_SCHEME_LIGHT
        AppCompatDelegate.MODE_NIGHT_YES -> COLOR_SCHEME_DARK
        else -> COLOR_SCHEME_SYSTEM
    }

    private fun isCustomTabsSupported(): Boolean {
        val packageName = CustomTabsClient.getPackageName(context, mutableListOf<String?>())
        return packageName != null
    }
}
