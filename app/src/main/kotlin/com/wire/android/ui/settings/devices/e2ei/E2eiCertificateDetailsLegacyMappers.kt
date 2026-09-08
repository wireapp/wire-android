/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId

internal fun E2EICertificateDetails.toNavigationPayload(): E2eiCertificateDetailsPayload =
    when (this) {
        is E2EICertificateDetails.DuringLoginCertificateDetails ->
            E2eiCertificateDetailsPayload.DuringLogin(certificate)

        is E2EICertificateDetails.AfterLoginCertificateDetails ->
            E2eiCertificateDetailsPayload.AfterLogin(
                certificate = mlsClientIdentity.x509Identity?.certificate.orEmpty(),
                userHandle = mlsClientIdentity.x509Identity?.handle?.handle.orEmpty(),
            )
    }

internal fun E2eiCertificateDetailsScreenNavArgs.toE2eiCertificateDetailsRoute(
    sessionId: WireSessionId,
    entryId: WireNavEntryId = WireNavEntryId.random(),
) = E2eiCertificateDetailsRoute(
    sessionId = sessionId,
    details = certificateDetails.toNavigationPayload(),
    entryId = entryId,
)
