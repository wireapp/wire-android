package com.wire.android.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.datadog.android.log.Logger
import com.wire.android.appLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

typealias LogElement = Triple<String, Severity, String?>

const val LOG_FILE_NAME = "wire_logs.txt"
private const val LOG_FILE_MAX_SIZE_THRESHOLD = 100 * 1024 * 1024 // 100MB
private const val BYTE_ARRAY_SIZE = 1024

@Suppress("TooGenericExceptionCaught", "BlockingMethodInNonBlockingContext")
class KaliumFileWriter : LogWriter() {

    private var flushCompleted = MutableStateFlow<Long>(0)

    val logTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val logFileTimeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    private val logBuffer = MutableStateFlow(LogElement("", Severity.Verbose, ""))
    private lateinit var filePath: String

    private var fileWriterCoroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val logger = Logger.Builder()
        .setNetworkInfoEnabled(true)
        .setLogcatLogsEnabled(true)
        .setDatadogLogsEnabled(true)
        .setBundleWithTraceEnabled(true)
        .setLoggerName("DATADOG")
        .build()

    suspend fun init(path: String) {

        path.let {
            filePath = try {
                getLogsDirectoryFromPath(it)
            } catch (e: FileNotFoundException) {
                // Fallback to default path
                it
            }
        }
        val logFile = getFile(filePath)

        /// Read some sort of unique iD per device and per user?

        coroutineScope {
            fileWriterCoroutineScope = this
            logBuffer.collect { logElement ->

                try {
                    writeToFile(logElement, logFile)
                } catch (e: Exception) {
                    appLogger.e("Write to file failed :${e}")
                }
                launch {
                    flushCompleted
                        .filter { filesize -> filesize > LOG_FILE_MAX_SIZE_THRESHOLD }
                        .collect {
                            compress(logFile)
                            clearFileContent(logFile)
                            FileWriter(logFile, true).use { fw ->
                                fw.flush()
                            }
                            writeToFile(logElement, logFile)
                        }
                }
            }
        }
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        fileWriterCoroutineScope.launch {
            logger.log(severity.ordinal, message)
            logBuffer.emit(LogElement(logTimeFormat.format(Date()), severity, message))
        }
    }

    fun clearFileContent(file: File) {
        val writer = PrintWriter(file)
        writer.print("")
        writer.close()
    }


    private suspend fun writeToFile(logElement: LogElement, file: File) {
        // Write to log
        FileWriter(file, true).use { fw ->
            // Write log lines to the file
            fw.append("${logElement.first}\t${logElement.second.name}\t${logElement.third}\n")

            fw.flush()
            // Validate file size
            flushCompleted.emit(file.length())
        }

        Runtime.getRuntime().exec("logcat -c ")
        Runtime.getRuntime().exec("logcat -f $file")
        flushCompleted.emit(file.length())


    }

    fun getFile(path: String): File {
        val file = File(path, LOG_FILE_NAME)

        if (!file.exists() && !file.createNewFile()) {
            throw IOException("Unable to load log file")
        }
        if (!file.canWrite()) {
            throw IOException("Log file not writable")
        }
        return file
    }

    private fun getLogsDirectoryFromPath(path: String): String {
        val dir = File(path, "logs")

        if (!dir.exists() && !dir.mkdirs()) {
            throw FileNotFoundException("Unable to create logs file")
        }
        return dir.absolutePath
    }

    fun deleteAllLogs(file: File) {
        file.parentFile.listFiles()
            ?.filter {
                it.extension.lowercase(Locale.ROOT) == "gz"
            }?.map { it.delete() }
    }

    @Suppress("NestedBlockDepth")
    private fun compress(file: File): Boolean {
        try {
            val childName =
                "${file.name.substringBeforeLast(".")}_${logFileTimeFormat.format(Date())}_${file.parentFile.listFiles().size}.gz"
            val compressed =
                File(
                    file.parentFile.absolutePath,
                    childName
                )
            FileInputStream(file).use { fis ->
                FileOutputStream(compressed).use { fos ->
                    GZIPOutputStream(fos).use { gzos ->

                        val buffer = ByteArray(BYTE_ARRAY_SIZE)
                        var length = fis.read(buffer)

                        while (length > 0) {
                            gzos.write(buffer, 0, length)

                            length = fis.read(buffer)
                        }

                        // Finish file compressing and close all streams.
                        gzos.finish()
                        clearFileContent(file)
                    }
                }
            }
        } catch (e: IOException) {
            appLogger.d("$e")
            return false
        }

        return true
    }
}
