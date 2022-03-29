package com.wire.android.ui.userprofile.other

data class OtherUserProfileScreenState(
    val avatarAssetByteArray: ByteArray? = null,
    val isAvatarLoading: Boolean = false,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String = "",
    val email: String = "",
    val phone: String = ""
)
