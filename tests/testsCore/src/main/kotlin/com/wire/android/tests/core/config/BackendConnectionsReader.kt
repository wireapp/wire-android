/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.tests.core.config

import com.wire.android.tests.core.utils.ZetaLogger
import org.json.JSONArray
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/*
 * This reader either reads the backend information from a file given via command line parameter (if present).
 * Or it reads the information by asking 1Password CLI through a shell script. The result is always a JSON.
 */
class BackendConnectionsReader {
    private val log: Logger = ZetaLogger.getLog(BackendConnectionsReader::class.simpleName)
    private val commandLineParameter: String = Config.common().getBackendConnections(BackendConnectionsReader::class.java)
    private val scriptTimeout: Duration = Duration.ofSeconds(60)

    fun read(): JSONArray {
        try {
            val pathname = if (commandLineParameter.isNotEmpty()) {
                log.info("Get backend connections from file: $commandLineParameter")
                commandLineParameter
            } else {
                createFileFrom1PasswordEntries()
            }
            val content = Files.readAllLines(File(pathname).toPath(), Charset.defaultCharset())
            return JSONArray(content)
        } catch (e: IOException) {
            throw RuntimeException("Could not read backend connections from file: ${e.message}", e)
        }
    }

    private fun createFileFrom1PasswordEntries(): String {
        log.info("Get backend connections via shell script from 1Password...")

        val tempScriptFile = try {
            val inputStream = Thread.currentThread().contextClassLoader
                ?.getResourceAsStream("backendConnections.sh")
                ?: throw FileNotFoundException("Could not find resource 'backendConnections.sh'")

            val tempFile = File.createTempFile("backendConnections", ".sh").apply {
                deleteOnExit()
            }

            inputStream.use { input ->
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            log.info("Copied backendConnections.sh to temporary file: ${tempFile.absolutePath}")
            tempFile
        } catch (e: IOException) {
            throw RuntimeException("Failed to copy backendConnections.sh to temporary file: ${e.message}", e)
        }

        try {
            val cmd = arrayOf("sh", tempScriptFile.absolutePath)
            log.info("Executing shell script: ${cmd.joinToString(" ")}")

            val process = ProcessBuilder(*cmd)
                .redirectErrorStream(true) // Optional: redirect stderr to stdout
                .start()

            // Log any error output
            outputErrorStreamToLog(process.errorStream)

            val finished = process.waitFor(scriptTimeout.toMillis(), TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw RuntimeException("Script timed out after ${scriptTimeout.toMillis()}ms")
            }

            if (process.exitValue() != 0) {
                throw RuntimeException("Shell script exited with code ${process.exitValue()}")
            }

        } catch (e: IOException) {
            throw RuntimeException("Could not execute shell script: ${e.message}", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Script execution interrupted: ${e.message}", e)
        }

        // Verify that the output file exists before returning
        val outputFile = Paths.get("").resolve("backendConnections.json").toFile()
        if (!outputFile.exists()) {
            throw RuntimeException("Expected output file backendConnections.json not found after script execution")
        }

        return outputFile.absolutePath
    }

    private fun outputErrorStreamToLog(stream: InputStream) {
        try {
            BufferedReader(InputStreamReader(stream)).use { br ->
                val sb = "\n"
                var s: String?
                while (br.readLine().also { s = it } != null) {
                    s += "\t"
                    s += s
                    s += "\n"
                }
                if (sb.trim().isNotEmpty()) {
                    log.warning(sb)
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Could not read error stream from shell script: ${e.message}", e)
        }
    }
}

//    companion object {
//        fun read(): JSONArray {
//            try {
//                val pathname = if (commandLineParameter.isNotEmpty()) {
//                    log.info("Get backend connections from file: $commandLineParameter")
//                    commandLineParameter
//                } else {
//                    createFileFrom1PasswordEntries()
//                }
//                val content = Files.readAllLines(File(pathname).toPath(), Charset.defaultCharset())
//                return JSONArray(content)
//            } catch (e: IOException) {
//                throw RuntimeException("Could not read backend connections from file: ${e.message}", e)
//            }
//        }
//    }
//}
