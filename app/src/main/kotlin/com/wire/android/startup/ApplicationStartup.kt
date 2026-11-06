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

package com.wire.android.startup

import com.wire.kalium.persistence.kmmSettings.MasterKeyInitializationException

internal enum class StartupState { Initializing, Ready, SecureStorageUnavailable }

/** Process-local state: a fresh process makes a fresh initialization attempt. */
internal class ApplicationStartup {
    @Volatile
    var state: StartupState = StartupState.Initializing
        private set

    @Suppress("TooGenericExceptionCaught")
    fun initialize(initializeDependencies: () -> Unit): Boolean = try {
        initializeDependencies()
        state = StartupState.Ready
        true
    } catch (failure: Exception) {
        if (!failure.isMasterKeyFailure()) throw failure
        state = StartupState.SecureStorageUnavailable
        false
    }

    private fun Throwable.isMasterKeyFailure(): Boolean {
        val visited = mutableSetOf<Throwable>()
        var current: Throwable? = this
        while (current != null && visited.add(current)) {
            if (current is MasterKeyInitializationException) return true
            current = current.cause
        }
        return false
    }
}
