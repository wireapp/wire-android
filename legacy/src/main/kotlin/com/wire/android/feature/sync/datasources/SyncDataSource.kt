package com.wire.android.feature.sync.datasources

import com.wire.android.feature.sync.SyncRepository
import com.wire.android.feature.sync.datasources.local.SyncLocalDataSource

class SyncDataSource(private val syncLocalDataSource: SyncLocalDataSource) : SyncRepository {

    override fun isSlowSyncRequired(): Boolean = syncLocalDataSource.isSlowSyncRequired()

    override fun setSlowSyncCompleted() = syncLocalDataSource.setSlowSyncCompleted()
}
