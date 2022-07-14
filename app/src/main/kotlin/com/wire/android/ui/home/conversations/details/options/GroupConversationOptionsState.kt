package com.wire.android.ui.home.conversations.details.options

data class GroupConversationOptionsState (
    val groupName: String = "",
    val isChangingAllowed: Boolean = false,
    val isTeamGroup: Boolean = false,
    val isGuestAllowed: Boolean = false,
    val isServicesAllowed: Boolean = false
)
