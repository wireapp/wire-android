/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.initialsync

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.ui.authentication.initialsync.InitialSyncGateway
import com.wire.android.ui.authentication.initialsync.InitialSyncGatewayResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/** App-owned Kalium/persistence implementation of the feature's initial-sync gateway. */
class KaliumInitialSyncGateway @Inject constructor(
    private val observeSyncState: ObserveSyncStateUseCase,
    private val userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount private val userId: UserId,
    private val dispatchers: DispatcherProvider,
    private val automatedLoginManager: AutomatedLoginManager,
) : InitialSyncGateway {
    override suspend fun awaitInitialSync(): InitialSyncGatewayResult = withContext(dispatchers.io()) {
        delay(DefaultDurationMillis.toLong()) // preserve the legacy smooth-transition delay
        if (observeSyncState().firstOrNull { it is SyncState.Live } == null) {
            appLogger.e("InitialSyncViewModel: SyncState is null")
            return@withContext InitialSyncGatewayResult.Unavailable
        }
        // Completion is emitted only after this durable marker has been written.
        userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
        InitialSyncGatewayResult.Completed(automatedLoginManager.consumePendingMoveToBackgroundAfterSync())
    }
}
