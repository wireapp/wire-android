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
package com.wire.android.ui.settings.devices.model

import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.kalium.logic.feature.e2ei.MLSClientIdentity

data class DeviceDetailsState(
    val device: Device = Device(),
    val isCurrentDevice: Boolean = false,
    val removeDeviceDialogState: RemoveDeviceDialogState = RemoveDeviceDialogState.Hidden,
    val error: RemoveDeviceError = RemoveDeviceError.None,
    val fingerPrint: String? = null,
    val isSelfClient: Boolean = false,
    val userName: String? = null,
    val isE2eiCertificateActivated: Boolean = false,
    val mlsClientIdentity: MLSClientIdentity? = null,
    val canBeRemoved: Boolean = false,
    val isLoadingCertificate: Boolean = false,
    val isE2EICertificateEnrollSuccess: Boolean = false,
    val isE2EICertificateEnrollError: Boolean = false,
    val isE2EIEnabled: Boolean = false,
    val startGettingE2EICertificate: Boolean = false,
    val mlsCipherSuiteSignature: String? = null,
    val deviceRemoved: Boolean = false,
    val isE2eiCertificateDataAvailable: Boolean = true,
)
