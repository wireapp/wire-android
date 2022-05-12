package com.wire.android.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.wire.android.appLogger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

typealias LogElement = Triple<String, Severity, String?>

const val LOG_FILE_NAME = "wire_logs.txt"
private const val LOG_FILE_MAX_SIZE_THRESHOLD = 5 * 1024 * 1024

@Suppress("TooGenericExceptionCaught", "BlockingMethodInNonBlockingContext")
class KaliumFileWriter : LogWriter() {

    private var flushCompleted = MutableStateFlow<Long>(0)

    val logTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val logBuffer = MutableStateFlow(LogElement("", Severity.Verbose, ""))
    private lateinit var filePath: String


    @OptIn(FlowPreview::class)
    fun init(path: String) {
        path.let {
            filePath = try {
                getLogsDirectoryFromPath(it)
            } catch (e: FileNotFoundException) {
                // Fallback to default path
                it
            }

        }
        val logFile = getFile(filePath)

        GlobalScope.launch {
            logBuffer.collect { logElement ->

                try {
                    writeToFile(logElement, logFile)
                } catch (e: Exception) {
                    appLogger.e("Write to file failed :${e}")
                }


                flushCompleted
                    .filter { filesize -> filesize > LOG_FILE_MAX_SIZE_THRESHOLD }
                    .collect {
                        clearFileContent(logFile)
                        FileWriter(logFile, true).use { fw ->
                            fw.flush()
                        }

                        writeToFile(logElement, logFile)
                    }

            }

        }

    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        GlobalScope.launch {
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
}
