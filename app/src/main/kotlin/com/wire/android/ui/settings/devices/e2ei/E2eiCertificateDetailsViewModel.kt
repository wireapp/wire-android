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
 */
package com.wire.android.ui.settings.devices.e2ei

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class E2eiCertificateDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    var state: E2eiCertificateDetailsState by mutableStateOf(E2eiCertificateDetailsState())
        private set

    private val e2eiCertificateDetailsScreenNavArgs: E2eiCertificateDetailsScreenNavArgs =
        savedStateHandle.navArgs()

    fun getCertificate() = e2eiCertificateDetailsScreenNavArgs.certificateString
}

data class E2eiCertificateDetailsState(
    val wireModalSheetState: WireModalSheetState = WireModalSheetState()
)
