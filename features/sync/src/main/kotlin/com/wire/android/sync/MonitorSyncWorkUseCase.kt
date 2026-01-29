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
package com.wire.android.sync

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.common.logger.kaliumLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
import com.wire.kalium.work.Work
import com.wire.kalium.work.WorkId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class MonitorSyncWorkUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:KaliumCoreLogic private val coreLogic: CoreLogic,
    private val allSessionsUseCase: ObserveSessionsUseCase
) {

    /**
     * Monitors if any user session has an ongoing Sync work being done, by:
     * 1. Gets all session scopes
     * 2. Gets the longWork scope from each session
     * 3. Get the observeNewWorks from each scope
     * 4. Merge the flows
     * 5. Launch a worker if any new work is of type InitialSync
     */
    suspend operator fun invoke() {
        allSessionsUseCase().filterIsInstance<GetAllSessionsResult.Success>().map { result ->
            result.sessions.map { session ->
                coreLogic.sessionScope(session.userId) { longWork }
            }
        }.flatMapLatest { scopes ->
            scopes.map { scope ->
                scope.observeNewWorks()
            }.merge()
        }.collect {
            if (it.type is Work.Type.InitialSync) {
                kaliumLogger.withTextTag("MonitorSyncWorkUseCase").i("Launching worker!")
                launchWorker(it.id)
            }
        }
    }

    private fun launchWorker(workId: WorkId) {
        val request = OneTimeWorkRequestBuilder<InitialSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .setInputData(InitialSyncWorker.createInputData(workId))
            .build()

        WorkManager.getInstance(context)
            .enqueue(request)
    }
}
