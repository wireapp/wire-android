package com.wire.android.ui.home.userprofile

import com.wire.android.ui.common.UserStatus

data class UserProfileState(
    val avatarUrl: String = "",
    val status: UserStatus,
    val fullName: String,
    val userName: String,
    val teamName: String, //maybe teamId is better here
    val otherAccounts: List<OtherAccount>
)

data class OtherAccount(val id: String, val avatarUrl: String, val fullName: String, val teamName: String? = null)
