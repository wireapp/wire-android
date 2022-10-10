package com.wire.android.ui.home.settings.account

data class MyAccountState(
    val fullName: String = "",
    val userName: String = "",
    val email: String = "",
    val teamName: String = "",
    val domain: String = "",
    val changePasswordUrl: String = ""
)
