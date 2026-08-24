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

import com.wire.android.ui.common.groupname.GroupNamePolicy
import com.wire.android.ui.common.groupname.GroupNamePolicyResult

object EditGroupNameValidator {
    fun onGroupNameChange(
        newText: String,
        currentState: EditConversationMetadataState,
    ): EditConversationMetadataState = when (GroupNamePolicy.evaluate(newText, currentState.originalGroupName)) {
        GroupNamePolicyResult.Empty -> currentState.copy(
            animatedGroupNameError = true,
            continueEnabled = false,
            error = EditConversationMetadataState.NameError.Empty,
        )

        GroupNamePolicyResult.TooLong -> currentState.copy(
            animatedGroupNameError = true,
            continueEnabled = false,
            error = EditConversationMetadataState.NameError.TooLong,
        )

        GroupNamePolicyResult.Unchanged -> currentState.copy(
            animatedGroupNameError = false,
            continueEnabled = false,
            error = EditConversationMetadataState.NameError.None,
        )

        GroupNamePolicyResult.Valid -> currentState.copy(
            animatedGroupNameError = false,
            continueEnabled = true,
            error = EditConversationMetadataState.NameError.None,
        )
    }

    fun onGroupNameErrorAnimated(
        currentState: EditConversationMetadataState,
    ): EditConversationMetadataState = currentState.copy(animatedGroupNameError = false)
}
