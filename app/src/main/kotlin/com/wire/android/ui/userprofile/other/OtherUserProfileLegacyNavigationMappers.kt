/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.other

import com.wire.android.ui.userprofile.toQualifiedId
import com.wire.android.ui.userprofile.toUserProfileQualifiedId
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId

internal fun OtherUserProfileNavArgs.toOtherUserProfileRoute(
    sessionId: WireSessionId,
    entryId: WireNavEntryId = WireNavEntryId.random(),
) = OtherUserProfileRoute(
    sessionId = sessionId,
    targetUserId = userId.toUserProfileQualifiedId(),
    groupConversationId = groupConversationId?.toUserProfileQualifiedId(),
    entryId = entryId,
)

internal fun OtherUserProfileRoute.toLegacyNavArgs() = OtherUserProfileNavArgs(
    userId = targetUserId.toQualifiedId(),
    groupConversationId = groupConversationId?.toQualifiedId(),
)

internal fun OtherUserProfileNavArgs.toViewModelArgs() = OtherUserProfileViewModelArgs(
    targetUserId = userId.toUserProfileQualifiedId(),
    groupConversationId = groupConversationId?.toUserProfileQualifiedId(),
)

internal fun String.toConnectionRequestIgnoredResult() = ConnectionRequestIgnoredResult(this)

internal fun ConnectionRequestIgnoredResult.toLegacyResult(): String = userName
