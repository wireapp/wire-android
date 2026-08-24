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

package com.wire.android.ui.home.conversations.details.metadata

data class EditConversationMetadataState(
    val originalGroupName: String = "",
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val isChannel: Boolean = true,
    val error: NameError = NameError.None,
    val completed: Completed = Completed.None,
) {
    enum class NameError {
        None, Empty, TooLong
    }

    enum class Completed {
        None, Success, Failure
    }
}
