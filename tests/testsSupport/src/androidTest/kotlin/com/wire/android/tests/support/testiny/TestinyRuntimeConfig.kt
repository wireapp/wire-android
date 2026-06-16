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

/**
 * Testiny values resolved from CI instrumentation args.
 * The API key should only come from runtime args, not source files.
 */
data class TestinyRuntimeConfig(
    val projectName: String,
    val runName: String,
    val apiKey: String,
    val sourceRunUrl: String? = null,
) {
    /**
     * True when the reporter has the project, run name, and API key needed for Testiny calls.
     */
    val isConfigured: Boolean
        get() = projectName.isNotBlank() && runName.isNotBlank() && apiKey.isNotBlank()
}
