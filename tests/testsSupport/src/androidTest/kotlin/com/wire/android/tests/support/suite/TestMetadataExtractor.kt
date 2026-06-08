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

import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.runner.Description

/** Extracts @TestCaseId and @Category from a JUnit description. */
object TestMetadataExtractor {
    fun from(description: Description): TestMetadata {
        val testCaseIds = description.annotations
            .filterIsInstance<TestCaseId>()
            .flatMap { it.value.asList() }
            .distinct()

        val categories = description.annotations
            .filterIsInstance<Category>()
            .flatMap { it.value.asList() }
            .distinct()

        return TestMetadata(
            testCaseIds = testCaseIds,
            categories = categories,
        )
    }
}
