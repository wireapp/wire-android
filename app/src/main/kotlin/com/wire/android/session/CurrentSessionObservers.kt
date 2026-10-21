/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.session

import com.wire.android.workmanager.worker.observeAnyUploadInProgress
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.call.RecentlyEndedCallMetadata
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Metadata selects the account; the SDK owns admission and same-account generation changes.
 * One-shot actions may wait through provider unavailability while raw Invalid/missing still cancels them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T : Any> Flow<CurrentSessionResult>.observeCurrentValidSession(
    coreLogic: CoreLogic,
    waitForAdmission: Boolean = false,
    observe: suspend UserSessionScope.() -> Flow<T>,
): Flow<T?> = flatMapLatest { result ->
    val account = (result as? CurrentSessionResult.Success)?.accountInfo
    if (account?.isValid() == true) {
        coreLogic.observeValidSession(account.userId, observe).let { admitted ->
            if (waitForAdmission) admitted.filterNotNull() else admitted
        }
    } else {
        // Invalid/missing/failure must reach latest-switching so the outgoing observation is cancelled.
        flowOf(null)
    }
}

internal fun CoreLogic.observeCurrentAssetUploadState(sessions: Flow<CurrentSessionResult>): Flow<Boolean> =
    sessions.observeCurrentValidSession(this) { observeAnyUploadInProgress() }.map { it == true }

internal fun CoreLogic.observeCurrentRecentlyEndedCalls(
    sessions: Flow<CurrentSessionResult>,
): Flow<RecentlyEndedCallMetadata?> = sessions.observeCurrentValidSession(this) {
    calls.observeRecentlyEndedCallMetadata()
}

/** Visibility is collected inside each admission, so a replay cannot reacquire an outgoing scope. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun CoreLogic.observeCallBackgroundState(
    sessions: Flow<GetAllSessionsResult>,
    visibility: Flow<Boolean>,
): Flow<Unit> = sessions.flatMapLatest { result ->
    val accounts = (result as? GetAllSessionsResult.Success)?.sessions.orEmpty().filter { it.isValid() }
    if (accounts.isEmpty()) {
        flowOf(Unit)
    } else {
        val observations = accounts.map { account ->
            observeValidSession(account.userId) {
                visibility.map { isVisible ->
                    // CallsScope is a fresh facade in the legacy API; preserve its per-visibility getter.
                    calls.setBackground(!isVisible)
                }
            }.onStart { emit(null) }
        }
        // Unavailable A must not keep ready B from observing visibility.
        combine(observations) { Unit }
    }
}

/** Runs one request at most once, waiting only while its raw account metadata stays valid. */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun CoreLogic.runWithValidSession(userId: UserId, action: suspend UserSessionScope.() -> Unit) {
    var started = false
    getGlobalScope().observeAllValidSessionsFlow().map { result ->
        (result as? GetAllSessionsResult.Success)?.sessions.orEmpty().any { it.userId == userId && it.isValid() }
    }.distinctUntilChanged().flatMapLatest { valid ->
        if (valid) {
            observeValidSession(userId) {
                flow {
                    if (!started) {
                        started = true
                        action()
                    }
                    emit(Unit)
                }
            }
        } else {
            flowOf(Unit)
        }
    }.filterNotNull().first()
}
