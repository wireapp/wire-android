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
package com.wire.android.ui.settings.devices.e2ei

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.util.fileDateTime
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class E2eiCertificateDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSelfUser: GetSelfUserUseCase,
) : ViewModel() {
    private val navArgs: E2eiCertificateDetailsScreenNavArgs =
        savedStateHandle.navArgs()

    private var selfUserHandle: String? = null

    init {
        getSelfUserHandle()
    }

    private fun getSelfUserHandle() {
        viewModelScope.launch {
            selfUserHandle = getSelfUser()?.handle
        }
    }

    fun getCertificate() =
        when (navArgs.certificateDetails) {
            is E2EICertificateDetails.DuringLoginCertificateDetails ->
                navArgs.certificateDetails.certificate

            is E2EICertificateDetails.AfterLoginCertificateDetails ->
                navArgs.certificateDetails.mlsClientIdentity.x509Identity?.certificate ?: ""
        }

    fun userHandle() =
        when (navArgs.certificateDetails) {
            is E2EICertificateDetails.DuringLoginCertificateDetails ->
                selfUserHandle

            is E2EICertificateDetails.AfterLoginCertificateDetails ->
                navArgs.certificateDetails.mlsClientIdentity.x509Identity?.handle?.handle ?: ""
        }

    fun getCertificateName(): String {
        val date = DateTimeUtil.currentInstant().fileDateTime()
        return "wire-certificate-${userHandle()}-$date.txt"
    }
}
