/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations

import com.wire.android.ui.debug.conversation.DebugConversationScreenNavArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavArgs
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminNavArgs
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNavArgs
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.mls.CipherSuite
import com.wire.navigation.WireSessionId
import kotlinx.datetime.Instant

internal fun ConversationAuxId.toQualifiedId() = QualifiedID(value, domain)
internal fun QualifiedID.toConversationAuxId() = ConversationAuxId(value, domain)

internal fun ConversationFoldersRoute.toViewModelArgs() = ConversationFoldersNavArgs(
    conversationId = conversationId.toQualifiedId(),
    conversationName = conversationName,
    currentFolderId = currentFolderId,
)

internal fun ConversationFoldersNavArgs.toNavigation3Route(sessionId: WireSessionId) = ConversationFoldersRoute(
    sessionId = sessionId,
    conversationId = conversationId.toConversationAuxId(),
    conversationName = conversationName,
    currentFolderId = currentFolderId,
)

internal fun SearchConversationMessagesRoute.toViewModelArgs() = SearchConversationMessagesNavArgs(
    conversationId = conversationId.toQualifiedId(),
    groupName = groupName,
    isCellsConversation = isCellsConversation,
)

internal fun PromoteAdminRoute.toViewModelArgs() = PromoteAdminNavArgs(
    conversationId = conversationId.toQualifiedId(),
    eligibleMembers = eligibleMembers
        .mapTo(ArrayList()) { "${it.value}@${it.domain}" },
)

internal fun AddMembersSearchRoute.toViewModelArgs() = AddMembersSearchNavArgs(
    conversationId = conversationId.toQualifiedId(),
    isConversationAppsEnabled = isConversationAppsEnabled,
    isSelfPartOfATeam = isSelfPartOfATeam,
    protocolInfo = protocol.toLegacy(),
    shouldUseNewAppsUi = shouldUseNewAppsUi,
)

internal fun DebugConversationRoute.toViewModelArgs() =
    DebugConversationScreenNavArgs(conversationId.toQualifiedId())

internal fun Conversation.ProtocolInfo.toNavigation3(): ConversationProtocolSelection = when (this) {
    Conversation.ProtocolInfo.Proteus -> ConversationProtocolSelection.Proteus
    is Conversation.ProtocolInfo.MLS -> ConversationProtocolSelection.Mls(
        groupId.value,
        groupState.toNavigation3(),
        epoch,
        keyingMaterialLastUpdate.toString(),
        cipherSuite.tag,
    )
    is Conversation.ProtocolInfo.Mixed -> ConversationProtocolSelection.Mixed(
        groupId.value,
        groupState.toNavigation3(),
        epoch,
        keyingMaterialLastUpdate.toString(),
        cipherSuite.tag,
    )
}

internal fun ConversationProtocolSelection.toLegacy(): Conversation.ProtocolInfo = when (this) {
    ConversationProtocolSelection.Proteus -> Conversation.ProtocolInfo.Proteus
    is ConversationProtocolSelection.Mls -> Conversation.ProtocolInfo.MLS(
        GroupID(groupId),
        groupState.toLegacy(),
        epoch,
        Instant.parse(keyingMaterialLastUpdate),
        CipherSuite.fromTag(cipherSuiteTag),
    )
    is ConversationProtocolSelection.Mixed -> Conversation.ProtocolInfo.Mixed(
        GroupID(groupId),
        groupState.toLegacy(),
        epoch,
        Instant.parse(keyingMaterialLastUpdate),
        CipherSuite.fromTag(cipherSuiteTag),
    )
}

private fun Conversation.ProtocolInfo.MLSCapable.GroupState.toNavigation3() =
    ConversationProtocolSelection.GroupState.valueOf(name)

private fun ConversationProtocolSelection.GroupState.toLegacy() =
    Conversation.ProtocolInfo.MLSCapable.GroupState.valueOf(name)
