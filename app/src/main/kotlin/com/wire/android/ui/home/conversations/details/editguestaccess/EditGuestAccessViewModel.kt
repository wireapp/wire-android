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

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_EDIT_GUEST_ACCESS_IS_GUEST_ACCESS_ALLOWED
import com.wire.android.navigation.EXTRA_EDIT_GUEST_ACCESS_IS_SERVICES_ALLOWED
import com.wire.android.navigation.EXTRA_EDIT_GUEST_ACCESS_IS_UPDATING_GUEST_ACCESS_ALLOWED
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditGuestAccessViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatcher: DispatcherProvider,
    private val updateConversationAccessRole: UpdateConversationAccessRoleUseCase,
    private val generateGuestRoomLink: GenerateGuestRoomLinkUseCase,
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper,
) : ViewModel() {

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        checkNotNull(savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)) {
            "No conversationId was provided via savedStateHandle to EditGuestAccessViewModel"
        }
    )
    private val isGuestAccessAllowed: Boolean = savedStateHandle.get<String>(EXTRA_EDIT_GUEST_ACCESS_IS_GUEST_ACCESS_ALLOWED)!!.toBoolean()
    private val isServicesAllowed: Boolean = savedStateHandle.get<String>(EXTRA_EDIT_GUEST_ACCESS_IS_SERVICES_ALLOWED)!!.toBoolean()
    private val isUpdatingGuestAccessAllowed: Boolean =
        savedStateHandle.get<String>(EXTRA_EDIT_GUEST_ACCESS_IS_UPDATING_GUEST_ACCESS_ALLOWED)!!.toBoolean()

    var editGuestAccessState by mutableStateOf(
        EditGuestAccessState(
            isGuestAccessAllowed = isGuestAccessAllowed,
            isUpdatingGuestAccessAllowed = isUpdatingGuestAccessAllowed
        )
    )

    private fun updateState(newState: EditGuestAccessState) {
        editGuestAccessState = newState
    }

    fun updateGuestAccess(shouldEnableGuestAccess: Boolean) {
        updateState(editGuestAccessState.copy(isUpdating = true, isGuestAccessAllowed = shouldEnableGuestAccess))
        when (shouldEnableGuestAccess) {
            true -> updateGuestRemoteRequest(shouldEnableGuestAccess)
            false -> updateState(editGuestAccessState.copy(changeGuestOptionConfirmationRequired = true))
        }
    }

    private fun updateGuestRemoteRequest(shouldEnableGuestAccess: Boolean) {
        viewModelScope.launch {
            withContext(dispatcher.io()) {
                updateConversationAccessRole(
                    allowGuest = shouldEnableGuestAccess,
                    allowNonTeamMember = shouldEnableGuestAccess,
                    allowServices = isServicesAllowed,
                    conversationId = conversationId
                )
            }.also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                        editGuestAccessState.copy(
                            isGuestAccessAllowed = !shouldEnableGuestAccess,
                            error = EditGuestAccessState.Error.Failure(it.cause)
                        )
                    )

                    is UpdateConversationAccessRoleUseCase.Result.Success -> Unit
                }
                updateState(editGuestAccessState.copy(isUpdating = false))
            }
        }
    }

    fun onGenerateGuestRoomLink() {
        viewModelScope.launch {
            editGuestAccessState = editGuestAccessState.copy(isGeneratingGuestRoomLink = true)
            generateGuestRoomLink(conversationId).also {
                editGuestAccessState = editGuestAccessState.copy(isGeneratingGuestRoomLink = false)
                if (it is GenerateGuestRoomLinkResult.Failure) {
                    editGuestAccessState = editGuestAccessState.copy(isFailedToGenerateGuestRoomLink = true)
                }
            }
        }
    }

    fun onGenerateGuestRoomFailureDialogDismiss() {
        editGuestAccessState = editGuestAccessState.copy(isFailedToGenerateGuestRoomLink = false)
    }

    fun onGuestDialogDismiss() {
        updateState(
            editGuestAccessState.copy(
                changeGuestOptionConfirmationRequired = false,
                isUpdating = false,
                isGuestAccessAllowed = !editGuestAccessState.isGuestAccessAllowed
            )
        )
    }

    fun onGuestDialogConfirm() {
        updateState(editGuestAccessState.copy(changeGuestOptionConfirmationRequired = false, isUpdating = true))
        updateGuestRemoteRequest(false)
    }

    fun navigateBack(args: Map<String, Boolean> = mapOf()) {
        viewModelScope.launch { navigationManager.navigateBack(args) }
    }
}
