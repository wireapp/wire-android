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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.android.ui.home.newconversation.channelaccess.toDomainEnum
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.feature.conversation.channel.UpdateChannelAddPermissionUseCase
import com.wire.kalium.logic.feature.conversation.channel.UpdateChannelAddPermissionUseCase.UpdateChannelAddPermissionUseCaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateChannelAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val updateChannelAddPermission: UpdateChannelAddPermissionUseCase,
    private val qualifiedIdMapper: QualifiedIdMapper,
) : ViewModel() {

    private val channelAccessNavArgs: UpdateChannelAccessArgs = savedStateHandle.navArgs()

    var accessType: ChannelAccessType by mutableStateOf(channelAccessNavArgs.accessType)
        private set

    var permissionType: ChannelAddPermissionType by mutableStateOf(channelAccessNavArgs.permissionType)
        private set

    val conversationId: String = channelAccessNavArgs.conversationId

    fun updateChannelAddPermission(newPermission: ChannelAddPermissionType) {
        viewModelScope.launch {
            val result = updateChannelAddPermission(
                channelAccessNavArgs.conversationId.toQualifiedID(qualifiedIdMapper),
                newPermission.toDomainEnum()
            )
            when (result) {
                is UpdateChannelAddPermissionUseCaseResult.Success -> {
                    permissionType = newPermission
                }

                is UpdateChannelAddPermissionUseCaseResult.Failure -> {
                    // TODO handle failure, show dialog or snackbar
                }
            }
        }
    }

    fun updateChannelAccess(newAccessType: ChannelAccessType) {
        accessType = newAccessType
        // TODO call use case to update the channel access
    }
}
