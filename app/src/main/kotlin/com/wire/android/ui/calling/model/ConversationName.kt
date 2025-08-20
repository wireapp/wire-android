/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.calling.model

import com.wire.android.R

sealed class ConversationName {
    data class Known(val name: String) : ConversationName()
    data class Unknown(val resourceId: Int) : ConversationName()
}

fun getConversationName(name: String?): ConversationName {
    return name?.let {
        ConversationName.Known(it)
    } ?: run {
        ConversationName.Unknown(R.string.calling_label_default_caller_name)
    }
}
