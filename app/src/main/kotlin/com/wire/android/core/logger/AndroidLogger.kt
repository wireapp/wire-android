package com.wire.android.core.logger

import android.util.Log

object AndroidLogger : Logger {
    override fun e(tag: String?, message: String) {
        Log.e(tag, message)
    }

    override fun w(tag: String?, message: String) {
        Log.w(tag, message)
    }

    override fun d(tag: String?, message: String) {
        Log.d(tag, message)
    }

    override fun i(tag: String?, message: String) {
        Log.i(tag, message)
    }

    override fun v(tag: String?, message: String) {
        Log.v(tag, message)
    }
}
