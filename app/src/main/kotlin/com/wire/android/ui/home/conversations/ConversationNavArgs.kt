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
package com.wire.android.ui.home.conversations

import android.os.Parcel
import android.os.Parcelable
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<ConversationId, QualifiedIdParceler>()
data class ConversationNavArgs(
    val conversationId: ConversationId,
    val searchedMessageId: String? = null,
    val pendingBundles: ArrayList<AssetBundle>? = null,
    val pendingTextBundle: String? = null,
    val threadId: String? = null,
    val threadRootMessageId: String? = null
) : Parcelable

object QualifiedIdParceler : Parceler<QualifiedID> {
    override fun create(parcel: Parcel) = parcel.readString().orEmpty().let {
        QualifiedID(it.substringBeforeLast("@"), it.substringAfterLast("@"))
    }
    override fun QualifiedID.write(parcel: Parcel, flags: Int) = parcel.writeString(this.value + "@" + this.domain)
}
