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
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.conversation.ConversationOptions
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

data class GroupMetadataState(
    val originalGroupName: String = "",
    val selectedUsers: ImmutableSet<Contact> = persistentSetOf(),
    val groupName: TextFieldValue = TextFieldValue(""),
    var groupProtocol: ConversationOptions.Protocol = ConversationOptions.Protocol.PROTEUS,
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val mlsEnabled: Boolean = true,
    val defaultProtocol: ConversationOptions.Protocol = ConversationOptions.Protocol.PROTEUS,
    val isLoading: Boolean = false,
    val error: NewGroupError = NewGroupError.None,
    val mode: GroupNameMode = GroupNameMode.CREATION,
    val isSelfTeamMember: Boolean? = null,
    val isGroupCreatingAllowed: Boolean? = null,
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
