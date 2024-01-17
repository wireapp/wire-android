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
package com.wire.android.ui.e2eiEnrollment

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.e2ei.GetE2EICertificateUseCase
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.debug.DebugDataOptionsState
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.E2EIFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class E2EIEnrollmentState(
    val certificate: String = "null",
    val showCertificate: Boolean = false
)

@HiltViewModel
class E2EIEnrollmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount private val userId: UserId,
    private val dispatchers: DispatcherProvider,
    private val e2eiCertificateUseCase: GetE2EICertificateUseCase
) : ViewModel() {
    var state by mutableStateOf(
        E2EIEnrollmentState()
    )
    val e2EIEnrollmentNavArgs: E2EIEnrollmentNavArgs = savedStateHandle.navArgs()

    fun enrollE2EICertificate(context: Context) {
        e2eiCertificateUseCase(context, ClientId(e2EIEnrollmentNavArgs.clientId)) { result ->
            result.fold({
                state = state.copy(
                    certificate = (it as E2EIFailure.FailedOAuth).reason, showCertificate = true
                )
            }, {
                if (it is E2EIEnrollmentResult.Finalized) {
                    state = state.copy(
                        certificate = it.certificate, showCertificate = true
                    )
                }
            })
        }
    }
    fun dismissCertificateDialog() {
        state = state.copy(
            showCertificate = false,
        )
    }
    fun waitUntilSyncIsCompleted(onCompleted: () -> Unit) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                observeSyncState().firstOrNull { it is SyncState.Live }
            }?.let {
                userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
                onCompleted()
            }
        }
    }
}
