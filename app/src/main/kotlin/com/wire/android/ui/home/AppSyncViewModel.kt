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
package com.wire.android.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.SavedStateViewModel
import com.wire.kalium.logic.feature.e2ei.CertificateRevocationListCheckWorker
import com.wire.kalium.logic.feature.e2ei.usecase.ObserveCertificateRevocationForSelfClientUseCase
import com.wire.kalium.logic.feature.featureConfig.FeatureFlagsSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSyncViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val certificateRevocationListCheckWorker: CertificateRevocationListCheckWorker,
    private val observeCertificateRevocationForSelfClient: ObserveCertificateRevocationForSelfClientUseCase,
    private val featureFlagsSyncWorker: FeatureFlagsSyncWorker
) : SavedStateViewModel(savedStateHandle) {

    fun startSyncingAppConfig() {
        viewModelScope.launch {
            certificateRevocationListCheckWorker.execute()
        }
        viewModelScope.launch {
            observeCertificateRevocationForSelfClient.invoke()
        }
        viewModelScope.launch {
            featureFlagsSyncWorker.execute()
        }
    }
}
