package com.wire.android.core.logger

import android.util.Log

class Logger {
    fun e(tag: String?, message: String) {
        Log.e(tag, message)
    }

    fun w(tag: String?, message: String) {
        Log.w(tag, message)
    }

    fun d(tag: String?, message: String) {
        Log.d(tag, message)
    }

    fun i(tag: String?, message: String) {
        Log.i(tag, message)
    }

    fun v(tag: String?, message: String) {
        Log.v(tag, message)
    }
}
