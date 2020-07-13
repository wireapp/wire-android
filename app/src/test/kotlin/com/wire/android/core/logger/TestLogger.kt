package com.wire.android.core.logger

private object TestLogger : Logger {
    override fun e(tag: String?, message: String) = log("E", tag, message)

    override fun w(tag: String?, message: String) = log("W", tag, message)

    override fun d(tag: String?, message: String) = log("D", tag, message)

    override fun i(tag: String?, message: String) = log("I", tag, message)

    override fun v(tag: String?, message: String) = log("V", tag, message)

    private fun log(level: String, tag: String?, message: String) = println("$level${tag?.let { "/$it" }}: $message")
}

class LoggerProvider {
    fun logger(): Logger = TestLogger
}
