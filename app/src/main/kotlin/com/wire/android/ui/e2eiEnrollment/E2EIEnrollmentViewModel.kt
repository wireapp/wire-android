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
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.e2ei.GetE2EICertificateUseCase
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.debug.DebugDataOptionsState
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.E2EIFailure
import com.wire.kalium.logic.data.client.ClientRepository
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.RegisterMLSClientUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.session.UpgradeCurrentSessionUseCase
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class E2EIEnrollmentState(
    val certificate: String = "null",
    val showCertificate: Boolean = false,
    val isLoading: Boolean = false,
    val isCertificateEnrollError: Boolean = false,
    val isCertificateEnrollSuccess: Boolean = false
)

@HiltViewModel
class E2EIEnrollmentViewModel @Inject constructor(
    private val e2eiCertificateUseCase: GetE2EICertificateUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
    var state by mutableStateOf(E2EIEnrollmentState())

    private val e2EIEnrollmentNavArgs: E2EIEnrollmentNavArgs = savedStateHandle.navArgs()

    fun enrollE2EICertificate(context: Context) {
        state = state.copy(isLoading = true)
        e2eiCertificateUseCase(context, ClientId(e2EIEnrollmentNavArgs.clientId)) { result ->
            result.fold({
                state = state.copy(
                    isLoading = false,
                    isCertificateEnrollError = true
                )
            }, {
                if (it is E2EIEnrollmentResult.Finalized) {
                    state = state.copy(
                        certificate = it.certificate,
                        isCertificateEnrollSuccess = true,
                        isCertificateEnrollError = false,
                        isLoading = false
                    )
                }
            })
        }
    }

    fun finishUp() {
        viewModelScope.launch {
            val clientScope = clientScopeProviderFactory.create(UserId(e2EIEnrollmentNavArgs.userId, e2EIEnrollmentNavArgs.userDomain)).clientScope
            clientScope.getOrRegister.invoke(ClientId(e2EIEnrollmentNavArgs.clientId))
        }
    }

    fun dismissErrorDialog() {
        state = state.copy(
            isCertificateEnrollError = false,
        )
    }
}
