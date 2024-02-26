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
package com.wire.android.ui.connection

import com.wire.kalium.logic.data.user.UserId

data class ConnectionActionState(
    val isPerformingAction: Boolean = false,
    val missingLegalHoldConsentDialogState: MissingLegalHoldConsentDialogState = MissingLegalHoldConsentDialogState.Hidden,
) {
    fun performAction() = copy(isPerformingAction = true)
    fun finishAction() = copy(isPerformingAction = false)
}

sealed class MissingLegalHoldConsentDialogState {
    data object Hidden : MissingLegalHoldConsentDialogState()
    data class Visible(val userId: UserId) : MissingLegalHoldConsentDialogState()
}
