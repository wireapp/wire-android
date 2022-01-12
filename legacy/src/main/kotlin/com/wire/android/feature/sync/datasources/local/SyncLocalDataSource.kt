package com.wire.android.feature.sync.datasources.local

import android.content.SharedPreferences

class SyncLocalDataSource(private val sharedPreferences: SharedPreferences) {

    fun isSlowSyncRequired(): Boolean = !sharedPreferences.getBoolean(SLOW_SYNC_COMPLETED, false)

    fun setSlowSyncCompleted() {
        sharedPreferences.edit().putBoolean(SLOW_SYNC_COMPLETED, true).apply()
    }

    companion object {
        private const val SLOW_SYNC_COMPLETED = "slow_sync_completed"
    }
}
