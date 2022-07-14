package com.wire.android.ui.home.newconversation.groupOptions

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.AccessRole.GUEST
import com.wire.kalium.logic.data.conversation.Conversation.AccessRole.NON_TEAM_MEMBER
import com.wire.kalium.logic.data.conversation.Conversation.AccessRole.SERVICE
import com.wire.kalium.logic.data.conversation.Conversation.AccessRole.TEAM_MEMBER

data class GroupOptionState(
    val continueEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isAllowGuestEnabled: Boolean = true,
    val isAllowServicesEnabled: Boolean = true,
    val isReadReceiptEnabled: Boolean = true,
    val showAllowGuestsDialog: Boolean = false,
    val accessRoleState: MutableSet<Conversation.AccessRole> = mutableSetOf(TEAM_MEMBER, NON_TEAM_MEMBER, GUEST, SERVICE)
)
