package com.wire.android.ui.userprofile.self.model


data class OtherAccount(
    val id: String,
    val avatarUrl: String,
    val fullName: String,
    val teamName: String? = null
)
