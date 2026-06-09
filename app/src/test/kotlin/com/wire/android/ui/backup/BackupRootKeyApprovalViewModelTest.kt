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

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.backup.ApproveBackupRootKeyRequestUseCase
import com.wire.kalium.logic.feature.backup.BackupRootKeyInfo
import com.wire.kalium.logic.feature.backup.BackupRootKeyRequestDecisionResult
import com.wire.kalium.logic.feature.backup.DeclineBackupRootKeyRequestUseCase
import com.wire.kalium.logic.feature.backup.ObservePendingBackupRootKeyRequestsUseCase
import com.wire.kalium.logic.feature.backup.PendingBackupRootKeyRequest
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class BackupRootKeyApprovalViewModelTest {

    @Test
    fun givenPendingRequest_whenObserved_thenDialogStateUsesClientLabel() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSelfClients(listOf(client(label = "Pixel")))
            .arrange()

        arrangement.pendingRequests.value = listOf(PENDING_REQUEST)
        advanceUntilIdle()

        assertEquals("Pixel", viewModel.state.request?.requesterName)
        assertEquals(REQUEST_ID, viewModel.state.request?.requestId)
    }

    @Test
    fun givenPendingRequestWithoutKnownClient_whenObserved_thenDialogStateUsesClientId() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        arrangement.pendingRequests.value = listOf(PENDING_REQUEST)
        advanceUntilIdle()

        assertEquals(REQUESTER_CLIENT_ID.value, viewModel.state.request?.requesterName)
    }

    @Test
    fun givenPendingRequest_whenApproved_thenApproveUseCaseIsInvoked() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        arrangement.pendingRequests.value = listOf(PENDING_REQUEST)
        advanceUntilIdle()

        viewModel.approve()
        advanceUntilIdle()

        assertEquals(REQUEST_ID to REQUESTER_CLIENT_ID, arrangement.approvedRequests.single())
    }

    @Test
    fun givenPendingRequest_whenDeclined_thenDeclineUseCaseIsInvoked() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        arrangement.pendingRequests.value = listOf(PENDING_REQUEST)
        advanceUntilIdle()

        viewModel.decline()
        advanceUntilIdle()

        assertEquals(REQUEST_ID to REQUESTER_CLIENT_ID, arrangement.declinedRequests.single())
    }

    @Test
    fun givenNoPendingRequest_whenObserved_thenDialogStateIsHidden() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        arrangement.pendingRequests.value = listOf(PENDING_REQUEST)
        advanceUntilIdle()

        arrangement.pendingRequests.value = emptyList()
        advanceUntilIdle()

        assertNull(viewModel.state.request)
    }

    private class Arrangement {
        val pendingRequests = MutableStateFlow<List<PendingBackupRootKeyRequest>>(emptyList())
        val approvedRequests = mutableListOf<Pair<String, ClientId>>()
        val declinedRequests = mutableListOf<Pair<String, ClientId>>()
        private var selfClients = emptyList<Client>()

        fun withSelfClients(clients: List<Client>) = apply {
            selfClients = clients
        }

        fun arrange(): Pair<Arrangement, BackupRootKeyApprovalViewModel> =
            this to BackupRootKeyApprovalViewModel(
                observePendingBackupRootKeyRequests = object : ObservePendingBackupRootKeyRequestsUseCase {
                    override fun invoke(): MutableStateFlow<List<PendingBackupRootKeyRequest>> = pendingRequests
                },
                approveBackupRootKeyRequest = object : ApproveBackupRootKeyRequestUseCase {
                    override suspend fun invoke(requestId: String, requesterClientId: ClientId): BackupRootKeyRequestDecisionResult {
                        approvedRequests.add(requestId to requesterClientId)
                        pendingRequests.value = emptyList()
                        return BackupRootKeyRequestDecisionResult.Success
                    }
                },
                declineBackupRootKeyRequest = object : DeclineBackupRootKeyRequestUseCase {
                    override suspend fun invoke(requestId: String, requesterClientId: ClientId): BackupRootKeyRequestDecisionResult {
                        declinedRequests.add(requestId to requesterClientId)
                        pendingRequests.value = emptyList()
                        return BackupRootKeyRequestDecisionResult.Success
                    }
                },
                fetchSelfClientsFromRemote = object : FetchSelfClientsFromRemoteUseCase {
                    override suspend fun invoke(): SelfClientsResult =
                        SelfClientsResult.Success(selfClients, currentClientId = null)
                },
                dispatchers = TestDispatcherProvider(),
            )
    }

    companion object {
        private const val REQUEST_ID = "request-id"
        private val REQUESTER_CLIENT_ID = ClientId("requester-client")
        private val NOW = Instant.parse("2026-01-01T00:00:00Z")
        private val PENDING_REQUEST = PendingBackupRootKeyRequest(
            requestId = REQUEST_ID,
            requesterClientId = REQUESTER_CLIENT_ID,
            requestedAt = NOW,
            backupRootKeyInfo = BackupRootKeyInfo(
                id = "root-key-id",
                fingerprint = "AA:BB",
                createdAt = NOW,
                createdByClientId = ClientId("creator-client"),
                version = 1,
            ),
        )

        private fun client(label: String? = null, model: String? = null): Client =
            Client(
                id = REQUESTER_CLIENT_ID,
                type = null,
                registrationTime = NOW,
                lastActive = NOW,
                isVerified = true,
                isValid = true,
                deviceType = null,
                label = label,
                model = model,
                mlsPublicKeys = null,
                isMLSCapable = true,
                isAsyncNotificationsCapable = true,
            )
    }
}
