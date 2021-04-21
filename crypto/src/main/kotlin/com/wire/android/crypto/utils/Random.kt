package com.wire.android.crypto.utils

import java.security.SecureRandom

object Random {
    fun long(): Long = SecureRandom().nextLong()
}
