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

package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.StringRes
import com.wire.android.R

sealed class ConversationSection : ConversationItemType {
    sealed class Predefined(@StringRes val sectionNameResId: Int) : ConversationSection() {
        data object Conversations : Predefined(R.string.conversation_label_conversations)
        data object Favorites : Predefined(R.string.conversation_label_favorites)
        data object NewActivities : Predefined(R.string.conversation_label_new_activity)
        data object BrowseChannels : Predefined(R.string.content_description_empty)
    }

    data class Custom(val sectionName: String) : ConversationSection()
    data object WithoutHeader : ConversationSection()
}

sealed interface ConversationItemType
