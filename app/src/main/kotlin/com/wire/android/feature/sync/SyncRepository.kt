package com.wire.android.feature.sync

interface SyncRepository {

    fun isSlowSyncRequired(): Boolean

    fun setSlowSyncCompleted()
}
