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
package com.wire.android.tests.support.suite

import android.util.Log
import io.qameta.allure.kotlin.Allure
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * JUnit Rule that captures logcat when a test FAILS
 * and attaches it to the Allure report as "logcat.txt".
 */
class AllureLogcatRule(
    private val maxLines: Int = 300 // how many lines of logcat to keep
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    // Run the actual test
                    base.evaluate()
                } catch (t: Throwable) {
                    // Test failed: try to capture logcat
                    try {
                        val logText = collectLogcat()
                        if (logText.isNotBlank()) {
                            attachLogcatToAllure(logText)
                        }
                    } catch (e: Throwable) {
                        // Never break the test because logcat collection failed
                        Log.e(
                            "AllureLogcatRule",
                            "Failed to collect logcat for ${description.displayName}",
                            e
                        )
                    }

                    // Re-throw the original failure so JUnit/Allure can mark it as FAILED
                    throw t
                }
            }
        }
    }

    /**
     * Attach the given logcat text as a .txt attachment in Allure.
     */
    private fun attachLogcatToAllure(logText: String) {
        Allure.lifecycle.addAttachment(
            "logcat",
            logText.byteInputStream(),
            "text/plain",
            "txt"
        )
    }

    /**
     * Runs "logcat -d -t <maxLines>" and returns the output as a String.
     *  - -d : dump and exit
     *  - -t N : output at most N lines
     */
    private fun collectLogcat(): String {
        val pid = android.os.Process.myPid()
        val process = Runtime.getRuntime().exec(
            arrayOf(
                "logcat",
                "-d",
                "-t",
                maxLines.toString(),
                "--pid=$pid"
            )
        )
        val builder = StringBuilder()

        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                builder.append(line).append('\n')
                line = reader.readLine()
            }
        }

        // clear logcat so next test doesn't get huge logs
        try {
            Runtime.getRuntime().exec("logcat -c")
        } catch (_: Throwable) {
            // ignore cleanup errors
        }

        return builder.toString()
    }
}
