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
package com.wire.android.feature.privacy.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable

/**
 * The base, user-chosen privacy level for a single conversation. These settings are stored
 * locally per user and are NOT synchronized with other participants nor with the backend.
 *
 * Ordinal order is the strength order (NORMAL < SENSITIVE < HIGHLY_SENSITIVE) and is relied on
 * when comparing/escalating levels.
 */
@Serializable
enum class ConversationPrivacyLevel {
    NORMAL,
    SENSITIVE,
    HIGHLY_SENSITIVE,
}

/**
 * Per-conversation auto-lock / auto-hide delay applied when the conversation loses focus
 * (background, device lock or inactivity timeout).
 */
@Serializable
enum class AutoLockTimeout(val delay: Duration) {
    IMMEDIATELY(Duration.ZERO),
    THIRTY_SECONDS(10.seconds),
    ONE_MINUTE(1.minutes),
    FIVE_MINUTES(5.minutes),
}

/**
 * The full local privacy configuration for a conversation. A conversation that has never been
 * configured is implicitly represented by the default instance ([ConversationPrivacyLevel.NORMAL]).
 */
@Serializable
data class ConversationPrivacySettings(
    val level: ConversationPrivacyLevel = ConversationPrivacyLevel.NORMAL,
    val autoLock: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY,
    /**
     * Whether this conversation is part of the "Panic list". When Panic Mode is ON, panic-listed
     * conversations are hidden (treated as at least Sensitive) regardless of their base [level], and
     * revert to their base level when Panic Mode is off. Lets the user bulk-hide a chosen set with one
     * switch instead of editing each conversation's level.
     */
    val panicProtected: Boolean = false,
) {
    val isDefault: Boolean get() = this == DEFAULT

    companion object {
        val DEFAULT = ConversationPrivacySettings()
    }
}
