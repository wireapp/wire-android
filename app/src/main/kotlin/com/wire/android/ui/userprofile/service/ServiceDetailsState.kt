package com.wire.android.ui.userprofile.service

import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

data class ServiceDetailsState(
    val userId: UserId,
    val conversationId: ConversationId? = null,
    val userAvatarAsset: ImageAsset.UserAvatarAsset? = null,
    val fullName: String = "",
    val userName: String = "",
    val membership: Membership = Membership.Service,
    val description: String = "",
    val summary: String = "",
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    val isDataLoading: Boolean = false,
    val isAvatarLoading: Boolean = false,
    val buttonState: ServiceDetailsButtonState = ServiceDetailsButtonState.HIDDEN
)

data class ServiceDetailsGroupState(
    val role: Conversation.Member.Role?,
    val isSelfAdmin: Boolean,
    val conversationId: ConversationId
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
