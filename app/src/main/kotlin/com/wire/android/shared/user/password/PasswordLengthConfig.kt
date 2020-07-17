package com.wire.android.shared.user.password

class PasswordLengthConfig {
    //TODO get these values from BuildConfig
    fun minLength() = MIN_LENGTH
    fun maxLength() = MAX_LENGTH

    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 120
    }
}
