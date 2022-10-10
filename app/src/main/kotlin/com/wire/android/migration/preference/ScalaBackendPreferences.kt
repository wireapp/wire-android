package com.wire.android.migration.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
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
    val blacklistUrl: String? get() = sharedPreferences.getString(BLACKLIST_HOST_PREF, null)
    val teamsUrl: String? get() = sharedPreferences.getString(TEAMS_URL_PREF, null)
    val accountsUrl: String? get() = sharedPreferences.getString(ACCOUNTS_URL_PREF, null)
    val websiteUrl: String? get() = sharedPreferences.getString(WEBSITE_URL_PREF, null)
    val apiVersion: String? get() = sharedPreferences.getString(API_VERSION_INFORMATION, null)

    companion object {
        private const val ENVIRONMENT_PREF = "CUSTOM_BACKEND_ENVIRONMENT"
        private const val BASE_URL_PREF = "CUSTOM_BACKEND_BASE_URL"
        private const val WEBSOCKET_URL_PREF = "CUSTOM_BACKEND_WEBSOCKET_URL"
        private const val BLACKLIST_HOST_PREF = "CUSTOM_BACKEND_BLACKLIST_HOST"
        private const val TEAMS_URL_PREF = "CUSTOM_BACKEND_TEAMS_URL"
        private const val ACCOUNTS_URL_PREF = "CUSTOM_BACKEND_ACCOUNTS_URL"
        private const val WEBSITE_URL_PREF = "CUSTOM_BACKEND_WEBSITE_URL"
        private const val CONFIG_URL_PREF = "CUSTOM_BACKEND_CONFIG_URL"
        private const val API_VERSION_INFORMATION = "API_VERSION_INFORMATION"
    }
}
