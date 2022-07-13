package com.wire.android.ui.home.newconversation.groupOptions

data class GroupOptionState(
    val continueEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isAllowGuestEnabled: Boolean = false,
    val isAllowServicesEnabled: Boolean = false,
    val isReadReceiptEnabled: Boolean = false,
)
