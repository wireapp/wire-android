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
package com.wire.android.emm

import android.os.Process

/**
 * Provides the current Android user context for multi-app MDM configurations.
 *
 * On Android, each user (including work profiles) has a unique user ID.
 * The user ID is calculated as UID / 100000, where UID is the process UID.
 * - User 0: Main user (UID 0-99999)
 * - User 10: Work profile or secondary user (UID 1000000-1099999)
 */
interface AndroidUserContextProvider {
    /**
     * Returns the current Android user ID.
     * This is calculated as Process.myUid() / 100000.
     *
     * @return The user ID (e.g., 0 for main user, 10 for work profile)
     */
    fun getCurrentAndroidUserId(): Int

    /**
     * Returns the current user ID as a string key for configuration lookup.
     *
     * @return The user ID as a string (e.g., "0", "10")
     */
    fun getCurrentUserIdKey(): String

    companion object {
        const val DEFAULT_KEY = "default"
        internal const val UID_DIVISOR = 100_000
    }
}

internal class AndroidUserContextProviderImpl : AndroidUserContextProvider {

    override fun getCurrentAndroidUserId(): Int =
        Process.myUid() / AndroidUserContextProvider.UID_DIVISOR

    override fun getCurrentUserIdKey(): String =
        getCurrentAndroidUserId().toString()
}
