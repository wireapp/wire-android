/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details.updatechannelaccess

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelPermissionType
import com.wire.android.ui.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpdateChannelAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val channelAccessNavArgs: UpdateChannelAccessArgs = savedStateHandle.navArgs()

    private val accessType: MutableState<ChannelAccessType> =
        mutableStateOf(channelAccessNavArgs.accessType)
    private val permissionType: MutableState<ChannelPermissionType> =
        mutableStateOf(channelAccessNavArgs.permissionType)

    fun getAccessType(): ChannelAccessType = accessType.value
    fun getPermissionType(): ChannelPermissionType = permissionType.value

    fun updateChannelPermission(newPermission: ChannelPermissionType) {
        permissionType.value = newPermission
        // TODO: call use case to update the channel permission
    }

    fun updateChannelAccess(newAccessType: ChannelAccessType) {
        accessType.value = newAccessType
        // TODO call use case to update the channel access
    }
}
