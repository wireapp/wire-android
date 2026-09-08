/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.qr

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId

internal fun SelfQrCodeNavArgs.toSelfQrCodeRoute(
    sessionId: WireSessionId,
    entryId: WireNavEntryId = WireNavEntryId.random(),
) = SelfQrCodeRoute(
    sessionId = sessionId,
    userHandle = userHandle,
    isTeamMember = isTeamMember,
    entryId = entryId,
)

internal fun SelfQrCodeRoute.toLegacyNavArgs() = SelfQrCodeNavArgs(
    userHandle = userHandle,
    isTeamMember = isTeamMember,
)

internal fun SelfQrCodeNavArgs.toViewModelArgs() = SelfQrCodeViewModelArgs(
    userHandle = userHandle,
    isTeamMember = isTeamMember,
)
