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

import kotlinx.serialization.Serializable

@Serializable
sealed class ConversationsSource {

    @Serializable
    data object MAIN : ConversationsSource()

    @Serializable
    data object ARCHIVE : ConversationsSource()

    @Serializable
    data object FAVORITES : ConversationsSource()

    @Serializable
    data object GROUPS : ConversationsSource()

    @Serializable
    data object CHANNELS : ConversationsSource()

    @Serializable
    data object ONE_ON_ONE : ConversationsSource()

    @Serializable
    data class FOLDER(val folderId: String, val folderName: String) : ConversationsSource()
}
