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

package com.wire.android.ui.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.backup.ApproveBackupRootKeyRequestUseCase
import com.wire.kalium.logic.feature.backup.DeclineBackupRootKeyRequestUseCase
import com.wire.kalium.logic.feature.backup.ObservePendingBackupRootKeyRequestsUseCase
import com.wire.kalium.logic.feature.backup.PendingBackupRootKeyRequest
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BackupRootKeyApprovalViewModel(
    private val observePendingBackupRootKeyRequests: ObservePendingBackupRootKeyRequestsUseCase,
    private val approveBackupRootKeyRequest: ApproveBackupRootKeyRequestUseCase,
    private val declineBackupRootKeyRequest: DeclineBackupRootKeyRequestUseCase,
    private val fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    var state: BackupRootKeyApprovalState by mutableStateOf(BackupRootKeyApprovalState())
        private set

    init {
        observePendingRequests()
    }

    fun approve() {
        val request = state.request ?: return
        viewModelScope.launch(dispatchers.io()) {
            state = state.copy(isLoading = true)
            approveBackupRootKeyRequest(request.requestId, request.requesterClientId)
        }
    }

    fun decline() {
        val request = state.request ?: return
        viewModelScope.launch(dispatchers.io()) {
            state = state.copy(isLoading = true)
            declineBackupRootKeyRequest(request.requestId, request.requesterClientId)
        }
    }

    private fun observePendingRequests() {
        viewModelScope.launch(dispatchers.io()) {
            observePendingBackupRootKeyRequests().collectLatest { pendingRequests ->
                val pendingRequest = pendingRequests.firstOrNull()
                state = if (pendingRequest == null) {
                    BackupRootKeyApprovalState()
                } else {
                    BackupRootKeyApprovalState(
                        request = pendingRequest.toUiRequest(resolveRequesterName(pendingRequest.requesterClientId)),
                    )
                }
            }
        }
    }

    private suspend fun resolveRequesterName(clientId: ClientId): String =
        when (val result = fetchSelfClientsFromRemote()) {
            is SelfClientsResult.Success -> result.clients
                .firstOrNull { it.id == clientId }
                ?.displayName()
                ?: clientId.value

            is SelfClientsResult.Failure -> clientId.value
        }

    private fun PendingBackupRootKeyRequest.toUiRequest(requesterName: String): BackupRootKeyApprovalRequest =
        BackupRootKeyApprovalRequest(
            requestId = requestId,
            requesterClientId = requesterClientId,
            requesterName = requesterName,
        )

    private fun Client.displayName(): String? =
        label?.takeIf { it.isNotBlank() }
            ?: model?.takeIf { it.isNotBlank() }
}

data class BackupRootKeyApprovalState(
    val request: BackupRootKeyApprovalRequest? = null,
    val isLoading: Boolean = false,
)

data class BackupRootKeyApprovalRequest(
    val requestId: String,
    val requesterClientId: ClientId,
    val requesterName: String,
)
