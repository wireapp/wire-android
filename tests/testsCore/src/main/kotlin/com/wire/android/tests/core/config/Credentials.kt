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
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.logging.Logger

class Credentials {
    private val log: Logger = ZetaLogger.getLog(Credentials::class.simpleName)

    /**
     * Retrieves a secret value either from environment variables or 1Password.
     * @param id The identifier for the secret (environment variable name or 1Password item name)
     * @return The secret value
     * @throws RuntimeException if the secret cannot be retrieved
     */
    fun getCredentials(id: String): String {
        return System.getenv(id)?.also {
            log.info("Received secret from environment variable $id")
        } ?: run {
            log.info("Please approve 1Password prompt to read $id...")
            readFrom1Password(id, "password")
        }
    }

    /**
     * Reads a secret from 1Password using the CLI.
     * @param id The 1Password item name
     * @param field The field name to retrieve (defaults to "password")
     * @return The secret value
     * @throws RuntimeException if 1Password CLI is not available or returns an error
     */
    private fun readFrom1Password(id: String, field: String = "password"): String {
        val builder = ProcessBuilder(
            "op", "item", "get", "--vault", "Test Automation",
            id, "--fields", field, "--reveal"
        ).apply {
            directory(File(System.getProperty("user.home")))
            redirectErrorStream(true) // Merge error stream with input stream
        }

        return try {
            val process = builder.start()
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()

            val exitCode = process.waitFor()
            when (exitCode) {
                0 -> output
                else -> throw RuntimeException(
                    "1Password CLI failed with exit code $exitCode for id '$id':\n$output"
                )
            }
        } catch (e: IOException) {
            throw RuntimeException(
                "1Password CLI not available. Please install it first. Error: ${e.message}",
                e
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt() // Restore the interrupted status
            throw RuntimeException("Operation interrupted while waiting for 1Password CLI", e)
        }
    }
}
