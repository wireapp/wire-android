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

/**
 * The single value every consumer (conversation list, chat screen, notifications, window flags,
 * auto-lock) reads. It combines a conversation's base [ConversationPrivacyLevel] with the global
 * Panic Mode state into one behaviour class, so the behaviour matrix lives in exactly one place
 * ([PrivacyResolver]) and cannot drift out of sync between features.
 */
enum class EffectivePrivacyLevel {
    /** Full content everywhere. */
    NORMAL,

    /** Hide previews, blur on focus loss, reveal by tap. No authentication. */
    SENSITIVE,

    /** SENSITIVE + blur immediately on any focus loss + secure window + hidden notification content. */
    SENSITIVE_PANIC,

    /** Authentication required to view, auto-lock, secure-session cleanup on lock. */
    HIGHLY_SENSITIVE,

    /** HIGHLY_SENSITIVE forced locked immediately + all metadata hidden. */
    HIGHLY_SENSITIVE_PANIC;

    /** Whether viewing the conversation requires authentication (biometric / device credential / Chat PIN). */
    val requiresAuth: Boolean
        get() = this == HIGHLY_SENSITIVE || this == HIGHLY_SENSITIVE_PANIC

    /** Whether the last-message preview must be hidden in the conversation list. */
    val hidesPreviewInList: Boolean
        get() = this != NORMAL

    /**
     * Whether the conversation identity (name/avatar) must be hidden in the conversation list.
     * Under Panic Mode even Sensitive conversations collapse to "Hidden Conversation" so the whole
     * list visibly locks down.
     */
    val hidesIdentityInList: Boolean
        get() = requiresAuth || this == SENSITIVE_PANIC

    /** Whether the window must be marked secure (FLAG_SECURE) while this conversation is open. */
    val needsSecureWindow: Boolean
        get() = this != NORMAL

    /** Whether Panic Mode is currently escalating this conversation's behaviour. */
    val isPanicEscalated: Boolean
        get() = this == SENSITIVE_PANIC || this == HIGHLY_SENSITIVE_PANIC
}