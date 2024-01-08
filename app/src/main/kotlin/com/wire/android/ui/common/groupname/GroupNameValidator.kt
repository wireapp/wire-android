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

import androidx.compose.ui.text.input.TextFieldValue

object GroupNameValidator {
    private const val GROUP_NAME_MAX_COUNT = 64

    /**
     * Receives a group field and state and returns the new state after validation
     */
    fun onGroupNameChange(newText: TextFieldValue, currentGroupState: GroupMetadataState): GroupMetadataState {
        val cleanText = newText.text.trim()
        return when {
            cleanText.isEmpty() -> {
                currentGroupState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError
                )
            }
            cleanText.count() > GROUP_NAME_MAX_COUNT -> {
                currentGroupState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.TextFieldError.GroupNameExceedLimitError
                )
            }
            cleanText == currentGroupState.originalGroupName -> {
                currentGroupState.copy(
                    animatedGroupNameError = false,
                    groupName = newText,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.None
                )
            }
            else -> {
                currentGroupState.copy(
                    animatedGroupNameError = false,
                    groupName = newText,
                    continueEnabled = true,
                    error = GroupMetadataState.NewGroupError.None
                )
            }
        }
    }

    fun onGroupNameErrorAnimated(currentGroupState: GroupMetadataState) = currentGroupState.copy(animatedGroupNameError = false)
}
