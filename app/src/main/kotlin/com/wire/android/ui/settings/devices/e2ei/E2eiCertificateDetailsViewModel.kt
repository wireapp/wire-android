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
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.fileDateTime
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.util.DateTimeUtil
import kotlinx.coroutines.launch
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.ui.home.settings.SettingsManualViewModelFactoryGroup

@WireAssistedViewModelBinding(SettingsManualViewModelFactoryGroup::class)
class E2eiCertificateDetailsViewModel @AssistedInject constructor(
    @Assisted private val navigationArgs: E2eiCertificateDetailsViewModelArgs,
    private val getSelfUser: GetSelfUserUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navigationArgs: E2eiCertificateDetailsViewModelArgs): E2eiCertificateDetailsViewModel
    }
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
        navigationArgs.certificate
    fun userHandle() =
        when (val args = navigationArgs) {
            is E2eiCertificateDetailsViewModelArgs.DuringLogin -> selfUserHandle
            is E2eiCertificateDetailsViewModelArgs.AfterLogin -> args.userHandle
        }
    fun getCertificateName(): String {
        val date = DateTimeUtil.currentInstant().fileDateTime()
        return "wire-certificate-${userHandle()}-$date.txt"
    }
}
