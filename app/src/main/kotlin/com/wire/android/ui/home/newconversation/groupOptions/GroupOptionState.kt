package com.wire.android.ui.home.newconversation.groupOptions

import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.conversation.ConversationOptions.AccessRole.GUEST
import com.wire.kalium.logic.data.conversation.ConversationOptions.AccessRole.NON_TEAM_MEMBER
import com.wire.kalium.logic.data.conversation.ConversationOptions.AccessRole.SERVICE
import com.wire.kalium.logic.data.conversation.ConversationOptions.AccessRole.TEAM_MEMBER

data class GroupOptionState(
    val continueEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isAllowGuestEnabled: Boolean = true,
    val isAllowServicesEnabled: Boolean = true,
    val isReadReceiptEnabled: Boolean = true,
    val accessRoleState: MutableSet<ConversationOptions.AccessRole> = mutableSetOf(TEAM_MEMBER, NON_TEAM_MEMBER, GUEST, SERVICE)
)
