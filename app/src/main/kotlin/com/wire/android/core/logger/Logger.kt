package com.wire.android.core.logger

interface Logger {
    fun e(tag: String?, message: String)
    fun w(tag: String?, message: String)
    fun d(tag: String?, message: String)
    fun i(tag: String?, message: String)
    fun v(tag: String?, message: String)
}

class LoggerProvider {
    fun logger(): Logger = AndroidLogger
}

val logger: Logger get() = LoggerProvider().logger()
