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

/** Testiny result values used by the sync client. */
enum class TestinyExecutionStatus(val apiValue: String) {
    Untested("NOTRUN"),
    Passed("PASSED"),
    Failed("FAILED"),
    Blocked("BLOCKED"),
    Skipped("SKIPPED");

    companion object {
        fun fromApiValue(value: String): TestinyExecutionStatus {
            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown Testiny execution status: $value")
        }
    }
}
