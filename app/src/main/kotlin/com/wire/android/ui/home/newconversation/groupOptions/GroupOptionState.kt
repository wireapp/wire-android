/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.newconversation.groupOptions

data class GroupOptionState(
    val continueEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isAllowGuestEnabled: Boolean = true,
    val isAllowServicesEnabled: Boolean = true,
    val isReadReceiptEnabled: Boolean = true,
    val showAllowGuestsDialog: Boolean = false,
    val error: Error? = null
) {
    sealed interface Error {
        object Unknown : Error
        object LackingConnection : Error
        data class ConflictedBackends(val domains: List<String>) : Error

        val isConflictedBackends get() = this is ConflictedBackends

    }
}
