/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.tests.support.testiny

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry

/** Reads Testiny runtime values from instrumentation arguments. */
object TestinyRuntimeConfigResolver {
    const val ARG_PROJECT_NAME = "testinyProjectName"
    const val ARG_RUN_NAME = "testinyRunName"
    const val ARG_API_KEY = "testinyApiKey"
    const val ARG_SOURCE_RUN_URL = "testinySourceRunUrl"

    fun fromInstrumentationArgs(arguments: Bundle = InstrumentationRegistry.getArguments()): TestinyRuntimeConfig? {
        val projectName = arguments.readTrimmed(ARG_PROJECT_NAME)
        val runName = arguments.readTrimmed(ARG_RUN_NAME)
        val apiKey = arguments.readTrimmed(ARG_API_KEY)
        val sourceRunUrl = arguments.readTrimmed(ARG_SOURCE_RUN_URL)
        val configuredValues = listOf(projectName, runName, apiKey, sourceRunUrl)

        // Treat a fully missing block as "Testiny disabled" for this run.
        if (configuredValues.all { it == null }) {
            return null
        }

        return TestinyRuntimeConfig(
            projectName = projectName.orEmpty(),
            runName = runName.orEmpty(),
            apiKey = apiKey.orEmpty(),
            sourceRunUrl = sourceRunUrl,
        )
    }

    private fun Bundle.readTrimmed(key: String): String? =
        getString(key)?.trim()?.takeIf { it.isNotEmpty() }
}
