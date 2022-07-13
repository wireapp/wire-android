package com.wire.android.ui.home.newconversation.groupOptions

data class GroupOptionState(
    val continueEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isAllowGuestEnabled: Boolean = true,
    val isAllowServicesEnabled: Boolean = true,
    val isReadReceiptEnabled: Boolean = true,
)
