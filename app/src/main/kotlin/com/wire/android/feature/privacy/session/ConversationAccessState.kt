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
package com.wire.android.feature.privacy.session

/**
 * Access state of an open conversation, produced by [SecureSessionManager.observeAccessState].
 *
 * - [Visible]: content may be shown (NORMAL, revealed SENSITIVE, or authenticated HIGHLY_SENSITIVE).
 * - [Concealed]: SENSITIVE content is blurred; a tap reveals it (no authentication).
 * - [Locked]: HIGHLY_SENSITIVE content is withheld until authentication succeeds.
 * - [Authenticating]: an authentication prompt is in progress.
 * - [AutoLockWarning]: HIGHLY_SENSITIVE content will auto-lock after inactivity; a countdown is shown
 *   and a tap ("keep active") cancels it. [durationMillis] is the length of the countdown.
 */
sealed interface ConversationAccessState {
    data object Visible : ConversationAccessState
    data object Concealed : ConversationAccessState
    data object Locked : ConversationAccessState
    data object Authenticating : ConversationAccessState
    data class AutoLockWarning(val durationMillis: Long) : ConversationAccessState
}