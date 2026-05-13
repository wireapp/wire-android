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
package com.wire.android.ui.userprofile.qr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SelfQRCodeViewModel.Factory::class)
class SelfQRCodeViewModel @AssistedInject constructor(
    @Assisted private val selfQrCodeNavArgs: SelfQrCodeNavArgs,
    @CurrentAccount private val selfUserId: UserId,
    private val selfServerLinks: SelfServerConfigUseCase,
    private val qrAssetRepository: SelfQRCodeAssetRepository,
    private val analyticsManager: AnonymousAnalyticsManager
) : ViewModel() {
    var selfQRCodeState by mutableStateOf(
        SelfQRCodeState(
            selfUserId,
            handle = selfQrCodeNavArgs.userHandle,
            isTeamMember = selfQrCodeNavArgs.isTeamMember
        )
    )
        private set

    init {
        viewModelScope.launch {
            getServerLinks()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: SelfQrCodeNavArgs): SelfQRCodeViewModel
    }

    suspend fun shareQRAsset(qrCodeImage: SelfQRCodeImage): String =
        qrAssetRepository.saveQRCode(qrCodeImage)

    fun trackAnalyticsEvent(event: AnalyticsEvent.QrCode.Modal) {
        analyticsManager.sendEvent(event)
    }

    private suspend fun getServerLinks() {
        selfQRCodeState =
            when (val result = selfServerLinks()) {
                is SelfServerConfigUseCase.Result.Failure -> selfQRCodeState.copy(hasError = true)
                is SelfServerConfigUseCase.Result.Success -> generateSelfUserUrls(result.serverLinks.links.accounts)
            }
    }

    private fun generateSelfUserUrls(accountsUrl: String): SelfQRCodeState =
        selfQRCodeState.copy(
            userAccountProfileLink = String.format(BASE_USER_PROFILE_URL, accountsUrl, selfUserId),
            userProfileLink = String.format(DIRECT_BASE_USER_PROFILE_URL, selfUserId.domain, selfUserId.value)
        )

    companion object {
        const val BASE_USER_PROFILE_URL = "%s/user-profile/?id=%s"

        const val DIRECT_BASE_USER_PROFILE_URL = "wire://user/%s/%s"
    }
}
