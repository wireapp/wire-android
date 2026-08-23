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

package com.wire.android.ui.common.groupname

object GroupNameValidator {
    /**
     * Receives a group field and state and returns the new state after validation
     */
    fun onGroupNameChange(newText: String, currentGroupState: GroupMetadataState): GroupMetadataState {
        return when (GroupNamePolicy.evaluate(newText, currentGroupState.originalGroupName)) {
            GroupNamePolicyResult.Empty -> {
                currentGroupState.copy(
                    animatedGroupNameError = true,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError
                )
            }

            GroupNamePolicyResult.TooLong -> {
                currentGroupState.copy(
                    animatedGroupNameError = true,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.TextFieldError.GroupNameExceedLimitError
                )
            }

            GroupNamePolicyResult.Unchanged -> {
                currentGroupState.copy(
                    animatedGroupNameError = false,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.None
                )
            }

            GroupNamePolicyResult.Valid -> {
                currentGroupState.copy(
                    animatedGroupNameError = false,
                    continueEnabled = true,
                    error = GroupMetadataState.NewGroupError.None
                )
            }
        }
    }

    fun onGroupNameErrorAnimated(currentGroupState: GroupMetadataState) = currentGroupState.copy(animatedGroupNameError = false)
}
