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

package com.wire.android.ui.home

import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import kotlin.time.Duration

data class FeatureFlagState(
    val showFileSharingDialog: Boolean = false,
    val isFileSharingEnabledState: Boolean = true,
    val fileSharingRestrictedState: SharingRestrictedState? = null,
    val shouldShowGuestRoomLinkDialog: Boolean = false,
    val shouldShowE2eiCertificateRevokedDialog: Boolean = false,
    val shouldShowTeamAppLockDialog: Boolean = false,
    val isTeamAppLockEnabled: Boolean = false,
    val isGuestRoomLinkEnabled: Boolean = true,
    val shouldShowSelfDeletingMessagesDialog: Boolean = false,
    val enforcedTimeoutDuration: SelfDeletionDuration = SelfDeletionDuration.None,
    val areSelfDeletedMessagesEnabled: Boolean = true,
    val e2EIRequired: E2EIRequired? = null,
    val e2EISnoozeInfo: E2EISnooze? = null,
    val e2EIResult: E2EIResult? = null,
    val isE2EILoading: Boolean = false,
    val showCallEndedBecauseOfConversationDegraded: Boolean = false,
    val startGettingE2EICertificate: Boolean = false
) {
    enum class SharingRestrictedState {
        NONE, NO_USER, RESTRICTED_IN_TEAM
    }

    data class E2EISnooze(val timeLeft: Duration)

    sealed class E2EIRequired {

        sealed class WithGracePeriod(open val timeLeft: Duration) : E2EIRequired() {
            data class Create(override val timeLeft: Duration) : WithGracePeriod(timeLeft)
            data class Renew(override val timeLeft: Duration) : WithGracePeriod(timeLeft)
        }

        sealed class NoGracePeriod : E2EIRequired() {
            data object Create : NoGracePeriod()
            data object Renew : NoGracePeriod()
        }
    }

    sealed class E2EIResult {
        data class Failure(val e2EIRequired: E2EIRequired) : E2EIResult()
        data class Success(val certificate: String) : E2EIResult()
    }
}
