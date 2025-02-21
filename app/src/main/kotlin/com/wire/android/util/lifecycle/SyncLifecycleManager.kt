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

package com.wire.android.util.lifecycle

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationManager
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.isLeft
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logger.KaliumLogger.Companion.ApplicationFlow.SYNC
import com.wire.kalium.logger.obfuscateDomain
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Provides tooling to simplify
 */
@Singleton
class SyncLifecycleManager @Inject constructor(
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val migrationManager: MigrationManager
) {

    private val logger by lazy { appLogger.withFeatureId(SYNC).withTextTag("SyncLifecycleManager") }

    /**
     * Starts observing the app state and take action.
     * Should be called only once on a global level
     */
    suspend fun observeAppLifecycle() {
        coreLogic.getGlobalScope().sessionRepository.allValidSessionsFlow()
            .filterIsInstance<Either.Right<List<AccountInfo>>>()
            .map { it.value }
            .combineTransform(migrationManager.isMigrationCompletedFlow()) { accounts, isMigrationCompleted ->
                if (isMigrationCompleted) {
                    emit(accounts)
                }
            }
            .combine(currentScreenManager.isAppVisibleFlow(), ::Pair)
            .distinctUntilChanged()
            .collectLatest { (accounts, isAppVisible) ->
                if (!isAppVisible) {
                    logger.i("Not running foreground sync request for users, as App is not visible.")
                    return@collectLatest
                }
                coroutineScope {
                    accounts.forEach { accountInfo ->
                        val userId = accountInfo.userId
                        launch {
                            logger.i("!!!!! Starting foreground sync request for user ${userId.value.obfuscateId()}.")
                            coreLogic.getSessionScope(userId).syncExecutor.request {
                                awaitCancellation()
                            }
                        }
                    }
                }
            }
    }

    /**
     * Attempts to perform sync and become up-to-date with remote.
     * After becoming online will hold the sync request for [stayAliveExtraDuration] more, before
     * releasing sync.
     * If there are more ongoing sync requests, this will
     */
    suspend fun syncTemporarily(userId: UserId, stayAliveExtraDuration: Duration = 0.seconds) {
        logger.d(
            "Handling connection policy for push notification of " +
                    "user=${userId.value.obfuscateId()}@${userId.domain.obfuscateDomain()}"
        )
        coreLogic.getSessionScope(userId).run {
            logger.d("Starting Sync request")
            syncExecutor.request {
                logger.d("Waiting until live")
                waitUntilLiveOrFailure().onFailure {
                    logger.w("Failed waiting until live")
                }.onSuccess {
                    delay(stayAliveExtraDuration)
                }
            }
        }
    }
}
