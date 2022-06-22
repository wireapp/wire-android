package com.wire.android.util

class WillNeverOccurError(message: String, throwable: Throwable?) : Error(message, throwable) {
    constructor(message: String) : this(message, null)
}
