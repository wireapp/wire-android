/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.migration.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaBackendPreferences @Inject constructor(@ApplicationContext private val applicationContext: Context) {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    val environment: String? get() = sharedPreferences.getString(ENVIRONMENT_PREF, null)
    val baseUrl: String? get() = sharedPreferences.getString(BASE_URL_PREF, null)
    val customConfigUrl: String? get() = sharedPreferences.getString(CONFIG_URL_PREF, null)
    val websocketUrl: String? get() = sharedPreferences.getString(WEBSOCKET_URL_PREF, null)
    val blacklistUrl: String? get() = sharedPreferences.getString(BLACKLIST_HOST_PREF, null).parseOptionString()
    val teamsUrl: String? get() = sharedPreferences.getString(TEAMS_URL_PREF, null)
    val accountsUrl: String? get() = sharedPreferences.getString(ACCOUNTS_URL_PREF, null)
    val websiteUrl: String? get() = sharedPreferences.getString(WEBSITE_URL_PREF, null)
    val apiVersion: String? get() = sharedPreferences.getString(API_VERSION_INFORMATION, null)

    // Some urls in scala app are wrapped in Option and actually stored as strings this way, so we have to parse them correctly.
    private fun String?.parseOptionString(): String? = this?.let {
        when {
            it == "None" -> null
            it.startsWith("Some(") && it.endsWith(")") -> it.removePrefix("Some(").removeSuffix(")")
            else -> it
        }
    }

    companion object {
        @VisibleForTesting const val ENVIRONMENT_PREF = "CUSTOM_BACKEND_ENVIRONMENT"
        @VisibleForTesting const val BASE_URL_PREF = "CUSTOM_BACKEND_BASE_URL"
        @VisibleForTesting const val WEBSOCKET_URL_PREF = "CUSTOM_BACKEND_WEBSOCKET_URL"
        @VisibleForTesting const val BLACKLIST_HOST_PREF = "CUSTOM_BACKEND_BLACKLIST_HOST"
        @VisibleForTesting const val TEAMS_URL_PREF = "CUSTOM_BACKEND_TEAMS_URL"
        @VisibleForTesting const val ACCOUNTS_URL_PREF = "CUSTOM_BACKEND_ACCOUNTS_URL"
        @VisibleForTesting const val WEBSITE_URL_PREF = "CUSTOM_BACKEND_WEBSITE_URL"
        @VisibleForTesting const val CONFIG_URL_PREF = "CUSTOM_BACKEND_CONFIG_URL"
        @VisibleForTesting const val API_VERSION_INFORMATION = "API_VERSION_INFORMATION"
    }
}
