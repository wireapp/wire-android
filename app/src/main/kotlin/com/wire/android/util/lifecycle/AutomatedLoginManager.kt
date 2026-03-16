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
package com.wire.android.util.lifecycle

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the app was started via an automated login intent,
 * so that the app can move to the background once initial sync completes.
 *
 * Intentionally in-memory only: if the process is killed mid-flow the flag
 * resets naturally, preventing the app from unexpectedly backgrounding itself
 * on a future normal launch.
 */
@Singleton
class AutomatedLoginManager @Inject constructor() {
    private val pendingMoveToBackgroundAfterSyncState = AtomicBoolean(false)

    val pendingMoveToBackgroundAfterSync: Boolean
        get() = pendingMoveToBackgroundAfterSyncState.get()

    fun markPendingMoveToBackgroundAfterSync() {
        pendingMoveToBackgroundAfterSyncState.set(true)
    }

    fun consumePendingMoveToBackgroundAfterSync(): Boolean =
        pendingMoveToBackgroundAfterSyncState.getAndSet(false)
}
