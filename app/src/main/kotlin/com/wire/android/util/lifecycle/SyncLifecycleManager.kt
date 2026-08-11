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
import com.wire.android.session.AppUserSessionPreparationResult
import com.wire.android.session.UserSessionPreparationGate
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger.Companion.ApplicationFlow.SYNC
import com.wire.kalium.logger.obfuscateDomain
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.sync.SyncRequestResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Provides tooling to simplify
 */
@SingleIn(AppScope::class)
class SyncLifecycleManager @Inject constructor(
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) {
    private val userSessionPreparationGate by lazy { UserSessionPreparationGate(coreLogic) }

    private val logger by lazy { appLogger.withFeatureId(SYNC).withTextTag("SyncLifecycleManager") }

    /**
     * Starts observing the app state and take action.
     * Should be called only once on a global level
     */
    suspend fun observeAppLifecycle() {
        coreLogic.getGlobalScope().observeAllValidSessionsFlow()
            .map { result ->
                when (result) {
                    is GetAllSessionsResult.Success -> result.sessions
                    is GetAllSessionsResult.Failure -> emptyList()
                }
            }
            .combine(currentScreenManager.isAppVisibleFlow()) { accounts, isAppVisible ->
                accounts to isAppVisible
            }
            .distinctUntilChanged()
            .collectLatest { (accounts, isAppVisible) ->
                logger.logAppSyncTelemetry(
                    event = AppSyncTelemetryEvent.APP_SYNC_VISIBILITY_CHANGED,
                    data = mapOf(
                        "visible" to isAppVisible,
                        "sessionCount" to accounts.size,
                    )
                )
                if (!isAppVisible) {
                    return@collectLatest
                }
                coroutineScope {
                    accounts.forEach { accountInfo ->
                        val userId = accountInfo.userId
                        launch {
                            val requestData = mapOf(
                                "trigger" to AppSyncTelemetryTrigger.APP_FOREGROUND.name,
                                "userId" to userId.toTelemetryString(),
                            )
                            logger.logAppSyncTelemetry(AppSyncTelemetryEvent.APP_SYNC_REQUEST_STARTED, requestData)
                            try {
                                val sessionScope = preparedSessionScope(userId) ?: return@launch
                                sessionScope.syncExecutor.request {
                                    awaitCancellation()
                                }
                            } finally {
                                logger.logAppSyncTelemetry(AppSyncTelemetryEvent.APP_SYNC_REQUEST_RELEASED, requestData)
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
    suspend fun syncTemporarily(
        userId: UserId,
        stayAliveExtraDuration: Duration = 0.seconds,
        waitForNextSyncState: Boolean = false
    ) {
        syncTemporarily(userId, stayAliveExtraDuration, waitForNextSyncState) {
            Unit
        }
    }

    /**
     * Attempts to perform sync, runs [actionWhenLive] after reaching live sync, then holds
     * the sync request for [stayAliveExtraDuration] before releasing it.
     */
    suspend fun syncTemporarily(
        userId: UserId,
        stayAliveExtraDuration: Duration = 0.seconds,
        waitForNextSyncState: Boolean = false,
        actionWhenLive: suspend () -> Unit
    ) {
        val requestData = mapOf(
            "trigger" to AppSyncTelemetryTrigger.PUSH_NOTIFICATION.name,
            "userId" to userId.toTelemetryString(),
            "stayAliveInMillis" to stayAliveExtraDuration.inWholeMilliseconds,
        )
        logger.logAppSyncTelemetry(AppSyncTelemetryEvent.APP_SYNC_REQUEST_STARTED, requestData)
        try {
            preparedSessionScope(userId)?.run {
                syncExecutor.request {
                    logger.d("Waiting until live")
                    val syncRequestResult = if (waitForNextSyncState) {
                        waitUntilNextLiveOrFailure()
                    } else {
                        waitUntilLiveOrFailure()
                    }
                    when (syncRequestResult) {
                        is SyncRequestResult.Failure -> logger.logAppSyncTelemetry(
                            event = AppSyncTelemetryEvent.APP_SYNC_WAIT_COMPLETED,
                            data = requestData + mapOf(
                                "outcome" to AppSyncTelemetryOutcome.FAILURE.name,
                                "failureType" to syncRequestResult.error::class.simpleName,
                            ),
                            level = KaliumLogLevel.WARN,
                        )

                        is SyncRequestResult.Success -> {
                            logger.logAppSyncTelemetry(
                                event = AppSyncTelemetryEvent.APP_SYNC_WAIT_COMPLETED,
                                data = requestData + ("outcome" to AppSyncTelemetryOutcome.SUCCESS.name),
                            )
                            actionWhenLive()
                            delay(stayAliveExtraDuration)
                        }
                    }
                }
            }
        } finally {
            logger.logAppSyncTelemetry(AppSyncTelemetryEvent.APP_SYNC_REQUEST_RELEASED, requestData)
        }
    }

    private fun UserId.toTelemetryString(): String =
        "${value.obfuscateId()}@${domain.obfuscateDomain()}"

    private suspend fun preparedSessionScope(userId: UserId) =
        when (val result = userSessionPreparationGate.prepare(userId)) {
            is AppUserSessionPreparationResult.Ready -> result.sessionScope
            is AppUserSessionPreparationResult.Failed -> {
                logger.w("Skipping sync because user-session preparation failed: ${result.reason}")
                null
            }
        }
}
