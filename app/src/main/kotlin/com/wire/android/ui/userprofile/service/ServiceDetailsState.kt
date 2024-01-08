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
package com.wire.android.ui.userprofile.service

import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.service.ServiceDetails
import com.wire.kalium.logic.data.service.ServiceId

data class ServiceDetailsState(
    val serviceId: ServiceId? = null,
    val conversationId: ConversationId? = null,
    val serviceDetails: ServiceDetails? = null,
    val serviceAvatarAsset: ImageAsset.UserAvatarAsset? = null,
    val isDataLoading: Boolean = false,
    val isAvatarLoading: Boolean = false,
    val buttonState: ServiceDetailsButtonState = ServiceDetailsButtonState.HIDDEN,
    val serviceMemberId: QualifiedID? = null
)

data class ServiceDetailsGroupState(
    val role: Conversation.Member.Role?,
    val isSelfAdmin: Boolean
)

enum class ServiceDetailsButtonState {
    /**
     * Add or Remove Service button should be hidden from UI
     */
    HIDDEN,

    /**
     * Button should be shown with Add Service specifications
     */
    ADD,

    /**
     * Button should be shown with Remove Service specifications
     */
    REMOVE
}
