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

import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.Tag
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter

/**
 * JUnit filter used by AndroidJUnitRunner / AllureAndroidJUnitRunner.
 *
 * Includes only tests that match instrumentation args:
 *  - testCaseId
 *  - category
 *  - tagKey / tagValue
 *
 * Tests that don't match are excluded from the run.
 */

class TaggedFilter : Filter() {

    // Read instrumentation arguments once
    private val args = InstrumentationRegistry.getArguments()

    private val filterTestCaseId: String? = args.getString("testCaseId")
    private val filterCategory: String? = args.getString("category")
    private val filterTagKey: String? = args.getString("tagKey")
    private val filterTagValue: String? = args.getString("tagValue")

    override fun shouldRun(description: Description): Boolean {
        // No filters -> include everything
        if (filterTestCaseId == null &&
            filterCategory == null &&
            filterTagKey == null &&
            filterTagValue == null
        ) {
            return true
        }

        // If this is a suite (class / package), run it if ANY child matches
        val children = description.children
        if (children.isNotEmpty()) {
            return children.any { shouldRun(it) }
        }

        // Leaf = actual test method
        return matchesFilters(description)
    }

    private fun matchesFilters(description: Description): Boolean {
        val annos = description.annotations

        // 1) TestCaseId
        filterTestCaseId?.let { wantedId ->
            val testCaseAnno = annos.filterIsInstance<TestCaseId>().firstOrNull()
            if (testCaseAnno == null || testCaseAnno.value != wantedId) {
                return false
            }
        }

        // 2) Category (Category(vararg val value: String))
        filterCategory?.let { wantedCat ->
            val cats = annos.filterIsInstance<Category>()
            if (cats.isEmpty()) return false

            val matchesCat = cats.any { catAnno ->
                catAnno.value.contains(wantedCat)
            }
            if (!matchesCat) return false
        }

        // 3) Tag (key + value)
        if (filterTagKey != null || filterTagValue != null) {
            val tags = annos.filterIsInstance<Tag>()
            if (tags.isEmpty()) return false

            val matchesTag = tags.any { tag ->
                val keyOk = filterTagKey?.let { it == tag.key } ?: true
                val valueOk = filterTagValue?.let { it == tag.value } ?: true
                keyOk && valueOk
            }
            if (!matchesTag) return false
        }

        // All conditions passed
        return true
    }

    override fun describe(): String {
        return "TaggedFilter(testCaseId=$filterTestCaseId, " +
                "category=$filterCategory, tagKey=$filterTagKey, tagValue=$filterTagValue)"
    }
}
