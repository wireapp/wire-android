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
package com.wire.android.util

import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.toUIText

/**
 * Cache for the current conversation details.
 * This is used to display the conversation name in the toolbar or can be used for other purposes.
 *
 * TODO: This is temporary, when we have navigation for sketch, we might do it with navigation arguments.
 * TODO: Anyway, this might be useful, and we might keep it or discuss it.
 */
object CurrentConversationDetailsCache {

    @Volatile
    var conversationName: UIText = "".toUIText()
        private set

    @Synchronized
    fun updateConversationName(newName: UIText) {
        conversationName = newName
    }
}
