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

import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.android.ui.home.newconversation.channelhistory.ChannelHistoryType
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.conversation.CreateConversationParam
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

data class GroupMetadataState(
    val originalGroupName: String = "",
    val selectedUsers: ImmutableSet<Contact> = persistentSetOf(),
    val groupProtocol: CreateConversationParam.Protocol = CreateConversationParam.Protocol.PROTEUS,
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isChannel: Boolean = true,
    val error: NewGroupError = NewGroupError.None,
    val mode: GroupNameMode = GroupNameMode.CREATION,
    val isSelfTeamMember: Boolean? = null,
    val isGroupCreatingAllowed: Boolean? = null,
    val isServicesAllowed: Boolean = false,
    val channelAccessType: ChannelAccessType = ChannelAccessType.PRIVATE,
    val channelAddPermissionType: ChannelAddPermissionType = ChannelAddPermissionType.ADMINS,
    val channelHistoryType: ChannelHistoryType = ChannelHistoryType.Off,
    val completed: Completed = Completed.None,
) {
    enum class Completed {
        None, Success, Failure
    }
    sealed interface NewGroupError {
        data object None : NewGroupError
        sealed interface TextFieldError : NewGroupError {
            data object GroupNameEmptyError : TextFieldError
            data object GroupNameExceedLimitError : TextFieldError
        }
    }
}

enum class GroupNameMode { CREATION, EDITION }
