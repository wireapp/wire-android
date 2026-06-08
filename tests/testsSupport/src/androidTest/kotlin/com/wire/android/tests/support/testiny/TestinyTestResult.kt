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

import com.wire.android.tests.support.suite.TestMetadata

/** Report payload for one executed UI test. */
data class TestinyTestResult(
    val testName: String,
    val metadata: TestMetadata,
    val status: TestinyExecutionStatus,
    val comment: String? = null,
) {
    // One executed test can update more than one Testiny case.
    val reportableTestCaseIds: List<String>
        get() = metadata.testCaseIds
            .map(::normalizeTestCaseId)
            .filter(String::isNotBlank)
            .distinct()

    val categories: List<String>
        get() = metadata.categories

    val hasReportableTestCaseIds: Boolean
        get() = reportableTestCaseIds.isNotEmpty()

    private fun normalizeTestCaseId(value: String): String {
        return value
            .trim()
            .removePrefix("@")
    }
}
