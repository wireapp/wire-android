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
package com.wire.android.tests.support.suite

/** Shared test annotations used by reporting and external sync. */
data class TestMetadata(
    val testCaseIds: List<String>,
    val categories: List<String>,
) {
    fun hasTestCaseId(value: String): Boolean = testCaseIds.contains(value)

    fun hasCategory(value: String): Boolean = categories.contains(value)
}
