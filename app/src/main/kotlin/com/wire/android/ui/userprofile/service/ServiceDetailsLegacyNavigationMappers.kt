/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.service

import com.wire.android.ui.userprofile.UserProfileQualifiedId
import com.wire.android.ui.userprofile.toQualifiedId
import com.wire.android.ui.userprofile.toUserProfileQualifiedId
import com.wire.kalium.logic.data.user.BotService
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId

internal fun ServiceDetailsNavArgs.toServiceDetailsRoute(
    sessionId: WireSessionId,
    entryId: WireNavEntryId = WireNavEntryId.random(),
) = ServiceDetailsRoute(
    sessionId = sessionId,
    conversationId = conversationId?.toUserProfileQualifiedId(),
    target = id.toServiceProfileTarget(),
    entryId = entryId,
)

internal fun ServiceDetailsRoute.toLegacyNavArgs() = ServiceDetailsNavArgs(
    conversationId = conversationId?.toQualifiedId(),
    id = target.toLegacyId(),
)

internal fun ServiceDetailsNavArgs.toViewModelArgs() = ServiceDetailsViewModelArgs(
    conversationId = conversationId?.toUserProfileQualifiedId(),
    target = id.toServiceProfileTarget(),
)

private fun ServiceDetailsNavArgs.Id.toServiceProfileTarget(): ServiceProfileTarget = when (this) {
    is ServiceDetailsNavArgs.Id.AppId -> ServiceProfileTarget.App(
        appId.toUserProfileQualifiedId()
    )

    is ServiceDetailsNavArgs.Id.BotServiceId -> ServiceProfileTarget.Bot(
        UserProfileQualifiedId(
            value = botService.id,
            domain = botService.provider,
        )
    )
}

private fun ServiceProfileTarget.toLegacyId(): ServiceDetailsNavArgs.Id = when (this) {
    is ServiceProfileTarget.App -> ServiceDetailsNavArgs.Id.AppId(id.toQualifiedId())
    is ServiceProfileTarget.Bot -> ServiceDetailsNavArgs.Id.BotServiceId(
        BotService(
            id = id.value,
            provider = id.domain,
        )
    )
}
