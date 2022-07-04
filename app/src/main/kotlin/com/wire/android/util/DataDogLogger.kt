package com.wire.android.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.datadog.android.log.Logger

class DataDogLogger: LogWriter() {

    private val logger = Logger.Builder()
        .setNetworkInfoEnabled(true)
        .setLogcatLogsEnabled(true)
        .setDatadogLogsEnabled(true)
        .setBundleWithTraceEnabled(true)
        .setLoggerName("DATADOG")
        .build()

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        logger.log(severity.ordinal, message, throwable)
    }
}
