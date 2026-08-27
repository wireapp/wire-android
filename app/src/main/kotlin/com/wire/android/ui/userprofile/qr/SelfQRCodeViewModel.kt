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
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import com.wire.content.media.EncodedImage
import com.wire.content.media.EncodedImageExportRequest
import com.wire.content.media.EncodedImageExporter
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.ui.home.settings.SettingsManualViewModelFactoryGroup
@WireAssistedViewModelBinding(SettingsManualViewModelFactoryGroup::class)
class SelfQRCodeViewModel @AssistedInject constructor(
    @Assisted private val navigationArgs: SelfQrCodeViewModelArgs,
    @CurrentAccount private val selfUserId: UserId,
    private val selfServerLinks: SelfServerConfigUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val encodedImageExporter: EncodedImageExporter,
    private val analyticsManager: AnonymousAnalyticsManager
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navigationArgs: SelfQrCodeViewModelArgs): SelfQRCodeViewModel
    }
    var selfQRCodeState by mutableStateOf(
        SelfQRCodeState(
            selfUserId,
            handle = navigationArgs.userHandle,
            isTeamMember = navigationArgs.isTeamMember
        )
    )
        private set
    init {
        viewModelScope.launch {
            getServerLinks()
        }
    }
    suspend fun shareQRAsset(image: EncodedImage): PlatformResult<ExternalContentReference> =
        encodedImageExporter.export(
            EncodedImageExportRequest(
                image = image,
                displayName = TEMP_SELF_QR_FILENAME,
                fallbackPath = kaliumFileSystem.rootCachePath / TEMP_SELF_QR_FILENAME,
            )
        )
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
        const val TEMP_SELF_QR_FILENAME = "temp_self_qr.jpg"
        const val BASE_USER_PROFILE_URL = "%s/user-profile/?id=%s"
        const val DIRECT_BASE_USER_PROFILE_URL = "wire://user/%s/%s"
        const val QR_QUALITY_COMPRESSION = 80
    }
}
