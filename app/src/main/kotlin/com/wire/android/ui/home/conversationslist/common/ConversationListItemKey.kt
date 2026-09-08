/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversationslist.common

import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationItemType
import com.wire.android.ui.home.conversationslist.model.ConversationSection

/**
 * Conversation IDs remain stable while paging refreshes. Section rows are transient separators,
 * and Paging can briefly expose the same separator more than once while generations are swapped.
 * Their position is therefore part of the key so that such a transient frame remains renderable.
 */
internal fun conversationListItemKey(item: ConversationItemType?, index: Int): String = when (item) {
    is ConversationItem -> item.conversationId.toString()
    is ConversationSection.Custom -> "section_custom_${item.sectionName}_$index"
    is ConversationSection.WithoutHeader -> "section_without_header_$index"
    ConversationSection.Predefined.BrowseChannels -> "section_predefined_browse_channels_$index"
    ConversationSection.Predefined.Conversations -> "section_predefined_conversations_$index"
    ConversationSection.Predefined.Favorites -> "section_predefined_favorites_$index"
    ConversationSection.Predefined.NewActivities -> "section_predefined_new_activities_$index"
    null -> "conversation_placeholder_$index"
}
