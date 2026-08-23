/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations.details

import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessNavArgs
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessParams
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkNavArgs
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesNavArgs
import com.wire.android.ui.home.conversations.details.metadata.EditConversationNameNavArgs
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsNavArgs
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessNavArgs
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessParams
import com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModelArgs
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.kalium.logic.data.id.QualifiedID

internal fun ConversationDetailsId.toQualifiedId() = QualifiedID(value, domain)
internal fun QualifiedID.toConversationDetailsId() = ConversationDetailsId(value, domain)

internal fun GroupConversationDetailsRoute.toViewModelArgs() =
    GroupConversationDetailsNavArgs(conversationId.toQualifiedId())

internal fun EditConversationNameRoute.toViewModelArgs() =
    EditConversationNameNavArgs(conversationId.toQualifiedId())

internal fun EditSelfDeletingMessagesRoute.toViewModelArgs() =
    EditSelfDeletingMessagesNavArgs(conversationId.toQualifiedId())

internal fun GroupConversationAllParticipantsRoute.toViewModelArgs() =
    GroupConversationAllParticipantsNavArgs(conversationId.toQualifiedId())

internal fun UpdateAppsAccessRoute.toViewModelArgs() = UpdateAppsAccessNavArgs(
    conversationId = conversationId.toQualifiedId(),
    updateAppsAccessParams = UpdateAppsAccessParams(
        isGuestAllowed = isGuestAllowed,
        isAppsAllowed = isAppsAllowed,
        shouldUseNewAppsUi = shouldUseNewAppsUi,
    ),
)

internal fun ChannelAccessOnUpdateRoute.toViewModelArgs() = UpdateChannelAccessViewModelArgs(
    conversationId = conversationId.toQualifiedId().toString(),
    accessType = accessType.toLegacy(),
    permissionType = permissionType.toLegacy(),
)

internal fun EditGuestAccessRoute.toViewModelArgs() = EditGuestAccessNavArgs(
    conversationId = conversationId.toQualifiedId(),
    editGuessAccessParams = EditGuestAccessParams(
        isGuestAccessAllowed = isGuestAccessAllowed,
        isServicesAllowed = isServicesAllowed,
        isUpdatingGuestAccessAllowed = isUpdatingGuestAccessAllowed,
    ),
)

internal fun CreatePasswordProtectedGuestLinkRoute.toViewModelArgs() =
    CreatePasswordGuestLinkNavArgs(conversationId.toQualifiedId())

internal fun ChannelAccessSelection.toLegacy() = when (this) {
    ChannelAccessSelection.PUBLIC -> ChannelAccessType.PUBLIC
    ChannelAccessSelection.PRIVATE -> ChannelAccessType.PRIVATE
}

internal fun ChannelPermissionSelection.toLegacy() = when (this) {
    ChannelPermissionSelection.ADMINS -> ChannelAddPermissionType.ADMINS
    ChannelPermissionSelection.EVERYONE -> ChannelAddPermissionType.EVERYONE
}

internal fun ChannelAccessType.toNavigation3() = when (this) {
    ChannelAccessType.PUBLIC -> ChannelAccessSelection.PUBLIC
    ChannelAccessType.PRIVATE -> ChannelAccessSelection.PRIVATE
}

internal fun ChannelAddPermissionType.toNavigation3() = when (this) {
    ChannelAddPermissionType.ADMINS -> ChannelPermissionSelection.ADMINS
    ChannelAddPermissionType.EVERYONE -> ChannelPermissionSelection.EVERYONE
}
