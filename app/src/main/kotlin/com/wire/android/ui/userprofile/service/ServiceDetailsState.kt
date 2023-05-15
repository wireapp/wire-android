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
