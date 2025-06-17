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
package com.wire.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.client.MLSClientManager
import com.wire.kalium.logic.feature.conversation.keyingmaterials.KeyingMaterialsManager
import com.wire.kalium.logic.feature.e2ei.SyncCertificateRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.ObserveCertificateRevocationForSelfClientUseCase
import com.wire.kalium.logic.feature.featureConfig.FeatureFlagsSyncWorker
import com.wire.kalium.logic.feature.mlsmigration.MLSMigrationManager
import com.wire.kalium.logic.feature.mls.MLSPublicKeysSyncWorker
import com.wire.kalium.logic.feature.server.UpdateApiVersionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class AppSyncViewModel @Inject constructor(
    private val syncCertificateRevocationListUseCase: SyncCertificateRevocationListUseCase,
    private val observeCertificateRevocationForSelfClient: ObserveCertificateRevocationForSelfClientUseCase,
    private val featureFlagsSyncWorker: FeatureFlagsSyncWorker,
    private val mlsPublicKeysSyncWorker: MLSPublicKeysSyncWorker,
    private val updateApiVersions: UpdateApiVersionsUseCase,
    private val mLSMigrationManager: MLSMigrationManager,
    private val keyingMaterialsManager: KeyingMaterialsManager,
    private val mLSClientManager: MLSClientManager,
    private val dispatcher: DispatcherProvider,
) : ViewModel() {

    private val minIntervalBetweenPulls: Duration = MIN_INTERVAL_BETWEEN_PULLS

    private var lastPullInstant: Instant? = null
    private var syncDataJob: Job? = null

    fun startSyncingAppConfig() {
        if (isSyncing()) return

        val now = Clock.System.now()
        if (isPullTooRecent(now)) return

        lastPullInstant = now
        syncDataJob = viewModelScope.launch(dispatcher.io()) {
            runSyncTasks()
        }
    }

    private fun isSyncing(): Boolean {
        return syncDataJob?.isActive == true
    }

    private fun isPullTooRecent(now: Instant): Boolean {
        return lastPullInstant?.let { lastPull ->
            lastPull + minIntervalBetweenPulls > now
        } ?: false
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runSyncTasks() {
        try {
            listOf(
                viewModelScope.launch(dispatcher.io()) { syncCertificateRevocationListUseCase() },
                viewModelScope.launch(dispatcher.io()) { featureFlagsSyncWorker.execute() },
                viewModelScope.launch(dispatcher.io()) { mlsPublicKeysSyncWorker.executeImmediately() },
                viewModelScope.launch(dispatcher.io()) { observeCertificateRevocationForSelfClient.invoke() },
                viewModelScope.launch(dispatcher.io()) { updateApiVersions() },
                viewModelScope.launch(dispatcher.io()) { mLSClientManager() },
                viewModelScope.launch(dispatcher.io()) { mLSMigrationManager() },
                viewModelScope.launch(dispatcher.io()) { keyingMaterialsManager() },
            ).joinAll()
        } catch (e: Exception) {
            appLogger.e("Error while syncing app config", e)
        }
    }

    companion object {
        val MIN_INTERVAL_BETWEEN_PULLS = 60.minutes
    }
}
