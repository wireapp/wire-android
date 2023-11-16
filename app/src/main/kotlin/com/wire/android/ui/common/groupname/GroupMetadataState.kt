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

package com.wire.android.ui.common.groupname

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.data.conversation.ConversationOptions

data class GroupMetadataState(
    val originalGroupName: String = "",
    val groupName: TextFieldValue = TextFieldValue(""),
    // TODO: this is var because the screen is changing it
    // this should be changed to val and only the view Model is allowed to change it
    var groupProtocol: ConversationOptions.Protocol = ConversationOptions.Protocol.PROTEUS,
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val mlsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: NewGroupError = NewGroupError.None,
    val mode: GroupNameMode = GroupNameMode.CREATION,
    val isSelfTeamMember: Boolean? = null
) {
    sealed interface NewGroupError {
        object None : NewGroupError
        sealed interface TextFieldError : NewGroupError {
            object GroupNameEmptyError : TextFieldError
            object GroupNameExceedLimitError : TextFieldError
        }
    }
}

enum class GroupNameMode { CREATION, EDITION }
