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
package com.wire.android.framework.fake

import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.sync.SyncExecutor
import com.wire.kalium.logic.sync.SyncRequest
import com.wire.kalium.util.DelicateKaliumApi

open class FakeSyncExecutor : SyncExecutor() {

    var waitUntilLiveCount = 0
    var requestCount = 0
    open fun onWaitUntilLiveOrFailure(): Either<CoreFailure, Unit> = Either.Right(Unit).also { waitUntilLiveCount++ }
    open fun onWaitUntilOrFailure(syncState: SyncState): Either<CoreFailure, Unit> = Either.Right(Unit)
    open fun onKeepSyncAlwaysOn() {}

    open fun onRequest() {requestCount++}

    override fun startAndStopSyncAsNeeded() = Unit

    override suspend fun <T> request(executorAction: suspend SyncRequest.() -> T): T {
        onRequest()
        return FakeSyncRequest().executorAction()
    }

    inner class FakeSyncRequest() : SyncRequest {
        override suspend fun waitUntilOrFailure(
            syncState: SyncState
        ): Either<CoreFailure, Unit> = onWaitUntilOrFailure(syncState)

        override suspend fun waitUntilLiveOrFailure(): Either<CoreFailure, Unit> = onWaitUntilLiveOrFailure()

        @DelicateKaliumApi(message = "By calling this, Sync will run indefinitely.")
        override fun keepSyncAlwaysOn() {
            onKeepSyncAlwaysOn()
        }
    }
}
