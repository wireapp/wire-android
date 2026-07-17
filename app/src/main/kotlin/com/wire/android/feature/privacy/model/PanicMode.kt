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
package com.wire.android.feature.privacy.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** How long Panic Mode stays active once enabled. */
enum class PanicDuration(val duration: Duration?) {
    FIFTEEN_MINUTES(15.minutes),
    ONE_HOUR(1.hours),

    /** Stays on until the user explicitly turns it off. */
    UNTIL_DISABLED(null);

    companion object {
        val DEFAULT = FIFTEEN_MINUTES
        fun fromNameOrDefault(name: String?): PanicDuration =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Runtime state of the global Panic Mode switch. */
sealed interface PanicModeState {
    data object Inactive : PanicModeState

    /**
     * Panic Mode is on. [expiresAtEpochMs] is the wall-clock deadline for auto-disable, or `null`
     * when the user picked "Until disabled". Persisting the deadline (not a live countdown) lets
     * Panic Mode survive process death and self-heal if it expired while the app was dead.
     */
    data class Active(val expiresAtEpochMs: Long?) : PanicModeState

    val isActive: Boolean get() = this is Active
}
