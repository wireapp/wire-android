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
@file:Suppress(
    "ReturnCount",
    "CyclomaticComplexMethod",
    "ComplexCondition"
)
package com.wire.android.tests.support.suite

import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.Tag
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter
import java.io.File

/**
 * JUnit filter used by AndroidJUnitRunner / AllureAndroidJUnitRunner.
 *
 * Includes only tests that match instrumentation args:
 *  - testCaseId
 *  - category
 *  - tagKey / tagValue
 *  - rerun list (Class#method) when rerun mode is enabled
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

    private val rerunModeEnabled: Boolean =
        args.getString(RetryContract.ARG_ENABLE_RERUN_MODE)?.equals("true", ignoreCase = true) == true
    private val rerunAttempt: Int =
        args.getString(RetryContract.ARG_RERUN_ATTEMPT)?.toIntOrNull() ?: 0
    private val rerunTestIds: Set<String> by lazy {
        loadRerunIds()
    }

    private val rerunModeActive: Boolean
        get() = rerunModeEnabled && rerunAttempt > 0

    override fun shouldRun(description: Description): Boolean {
        if (rerunModeActive) {
            return shouldRunInRerunMode(description)
        }

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

    private fun shouldRunInRerunMode(description: Description): Boolean {
        val children = description.children
        if (children.isNotEmpty()) {
            return children.any { shouldRunInRerunMode(it) }
        }

        if (rerunTestIds.isEmpty()) {
            throw IllegalStateException(
                "Rerun mode is enabled but no retry tests were provided " +
                        "(${RetryContract.ARG_RERUN_LIST_PATH}/${RetryContract.ARG_RERUN_LIST_INLINE})."
            )
        }

        val testId = toTestId(description) ?: return false
        return rerunTestIds.contains(testId)
    }

    private fun toTestId(description: Description): String? {
        val className = description.className ?: return null
        val methodName = description.methodName ?: return null
        return "$className#$methodName"
    }

    private fun loadRerunIds(): Set<String> {
        if (!rerunModeActive) return emptySet()

        val ids = linkedSetOf<String>()
        // CI may split large rerun lists into multiple instrumentation args.
        parseIds(args.getString(RetryContract.ARG_RERUN_LIST_INLINE)).forEach { ids.add(it) }
        loadInlinePartKeys()
            .forEach { key ->
                parseIds(args.getString(key)).forEach { ids.add(it) }
            }

        val path = args.getString(RetryContract.ARG_RERUN_LIST_PATH)?.trim().orEmpty()
        if (path.isNotEmpty()) {
            try {
                parseIds(File(path).readText()).forEach { ids.add(it) }
            } catch (_: Throwable) {
                throw IllegalStateException("Failed to read rerun list from path: '$path'")
            }
        }
        return ids
    }

    private fun loadInlinePartKeys(): List<String> {
        val prefix = RetryContract.ARG_RERUN_LIST_INLINE_PART_PREFIX
        return args.keySet()
            .filter { it.startsWith(prefix) }
            .sortedWith(
                compareBy<String> { key ->
                    key.removePrefix(prefix).toIntOrNull() ?: Int.MAX_VALUE
                }.thenBy { key -> key }
            )
    }

    private fun parseIds(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw
            .split(",", "\n", "\r")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .onEach { value ->
                if (!value.contains('#')) {
                    throw IllegalStateException(
                        "Invalid rerun test id '$value'. Expected format: ClassName#methodName."
                    )
                }
            }
            .toSet()
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

        return true
    }

    override fun describe(): String {
        return "TaggedFilter(testCaseId=$filterTestCaseId, " +
                "category=$filterCategory, tagKey=$filterTagKey, tagValue=$filterTagValue, " +
                "rerunModeEnabled=$rerunModeEnabled, rerunAttempt=$rerunAttempt)"
    }
}
