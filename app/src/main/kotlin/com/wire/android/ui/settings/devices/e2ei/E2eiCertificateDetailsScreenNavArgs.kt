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

import com.wire.kalium.logic.feature.e2ei.E2eiCertificate
import kotlinx.serialization.Serializable

data class E2eiCertificateDetailsScreenNavArgs(val certificateDetails: E2EICertificateDetails)

@Serializable
sealed class E2EICertificateDetails {
    @Serializable
    data class AfterLoginCertificateDetails(val certificate: E2eiCertificate) : E2EICertificateDetails()
    @Serializable
    data class DuringLoginCertificateDetails(val certificate: String) : E2EICertificateDetails()
}
