package com.wire.android.ui.common.groupname

import androidx.compose.ui.text.input.TextFieldValue

object GroupNameValidator {
    private const val GROUP_NAME_MAX_COUNT = 64

    /**
     * Receives a group field and state and returns the new state after validation
     */
    fun onGroupNameChange(newText: TextFieldValue, currentGroupState: GroupMetadataState): GroupMetadataState {
        return when {
            newText.text.trim().isEmpty() -> {
                currentGroupState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError
                )
            }
            newText.text.trim().count() > GROUP_NAME_MAX_COUNT -> {
                currentGroupState.copy(
                    animatedGroupNameError = true,
                    groupName = newText,
                    continueEnabled = false,
                    error = GroupMetadataState.NewGroupError.TextFieldError.GroupNameExceedLimitError
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
