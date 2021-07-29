package com.wire.android.core.events.datasource.local

import android.content.SharedPreferences

class NotificationLocalDataSource(private val sharedPreferences: SharedPreferences) {

    fun lastNotificationId(): String? = sharedPreferences.getString(LAST_NOTIFICATION_ID, null)

    fun saveLastNotificationId(notificationId: String) {
        sharedPreferences.edit().putString(LAST_NOTIFICATION_ID, notificationId).apply()
    }

    companion object {
        private const val LAST_NOTIFICATION_ID = "last_notification_id"
    }
}
