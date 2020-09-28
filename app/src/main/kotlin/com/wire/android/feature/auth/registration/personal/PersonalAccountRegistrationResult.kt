package com.wire.android.feature.auth.registration.personal

import com.wire.android.shared.user.User

data class PersonalAccountRegistrationResult(
    val user: User,
    val refreshToken: String
)
