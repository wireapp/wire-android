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

import android.os.Build
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.Tag
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter
import java.io.File
import kotlin.math.abs

/**
 * JUnit filter used by AndroidJUnitRunner / AllureAndroidJUnitRunner.
 *
 * Supports normal selector args (@TestCaseId, @Category, @Tag, excludeCategory,
 * requiredCategory), and rerun selector args (Class#method). excludeCategory
 * skips tests that also have that category, for example normal test phases skip
 * upgrade tests. requiredCategory lets CI keep the original selector and add one
 * more category requirement for a second pass, for example regression + upgrade.
 */
class TaggedFilter : Filter() {
    private val args = InstrumentationRegistry.getArguments()

    private val filterTestCaseId: String? = args.getString("testCaseId")
    private val filterCategory: String? = args.getString("category")
    private val excludeCategory: String? = args.getString("excludeCategory")
    private val requiredCategory: String? = args.getString("requiredCategory")
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
        if (description.children.isNotEmpty()) {
            return description.children.any { shouldRun(it) }
        }
        if (!matchesSelection(description)) return false
        val minSdk = description.annotations.filterIsInstance<Tag>().firstOrNull { it.key == "minSdk" }
            ?.value?.let { value ->
                requireNotNull(value.toIntOrNull()?.takeIf { it > 0 }) { "Invalid minSdk tag: $value" }
            } ?: 0
        if (args.getString("ciListDeviceRequirements") == "true") {
            check(args.getString("log") == "true") { "Device requirement discovery must use log-only mode" }
            val testId = toTestId(description)
            if (minSdk > 0 && testId != null) {
                val status = Bundle().apply { putString("stream", "CI_TEST_MIN_SDK:$testId\t$minSdk\n") }
                InstrumentationRegistry.getInstrumentation().sendStatus(0, status)
            }
            return true
        }
        return matchesDeviceShard(description, minSdk)
    }

    /**
     * Tests tagged with minSdk require a phone running that Android SDK or later.
     * Keep the usual shard for other tests, and reassign a tagged test only when
     * its original phone is too old. CI uses the same requirement when planning retries.
     */
    private fun matchesDeviceShard(description: Description, minSdk: Int): Boolean {
        val deviceSdks = args.getString("ciDeviceSdks")?.split(',')?.map(String::toInt)
            ?: return Build.VERSION.SDK_INT >= minSdk
        val deviceIndex = requireNotNull(args.getString("ciDeviceIndex")).toInt()
        val eligible = deviceSdks.indices.filter { deviceSdks[it] >= minSdk }
        check(eligible.isNotEmpty()) { "No device supports SDK $minSdk for ${toTestId(description)}" }
        if (args.getString("ciRunOnAllDevices") == "true") return deviceIndex in eligible

        val hash = abs(description.hashCode().toLong())
        val originalShard = (hash % deviceSdks.size).toInt()
        val assignedShard = if (originalShard in eligible) originalShard else eligible[(hash % eligible.size).toInt()]
        return deviceIndex == assignedShard
    }

    private fun matchesSelection(description: Description): Boolean {
        if (matchesExcludedCategory(description)) return false
        return if (rerunModeActive) shouldRunInRerunMode(description) else matchesFilters(description)
    }

    private fun shouldRunInRerunMode(description: Description): Boolean {
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
                // Sort part2 before part10.
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
                // Fail fast on malformed retry IDs.
                if (!value.contains('#')) {
                    throw IllegalStateException(
                        "Invalid rerun test id '$value'. Expected format: ClassName#methodName."
                    )
                }
            }
            .toSet()
    }

    private fun matchesFilters(description: Description): Boolean {
        val annotations = description.annotations
        val categories = annotations.filterIsInstance<Category>()

        // 1) TestCaseId
        filterTestCaseId?.let { wantedId ->
            val testCaseAnno = annotations.filterIsInstance<TestCaseId>().firstOrNull()
            if (testCaseAnno == null || !testCaseAnno.value.contains(wantedId)) {
                return false
            }
        }

        // 2) Category (Category(vararg val value: String))
        filterCategory?.let { wantedCat ->
            if (categories.isEmpty()) return false

            val matchesCat = categories.any { catAnno ->
                catAnno.value.contains(wantedCat)
            }
            if (!matchesCat) return false
        }

        requiredCategory?.let { requiredCat ->
            if (categories.isEmpty()) return false

            val matchesRequiredCat = categories.any { catAnno ->
                catAnno.value.contains(requiredCat)
            }
            if (!matchesRequiredCat) return false
        }

        // 3) Tag (key + value)
        if (filterTagKey != null || filterTagValue != null) {
            val tags = annotations.filterIsInstance<Tag>()
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

    private fun matchesExcludedCategory(description: Description): Boolean =
        excludeCategory?.let { excluded ->
            description.annotations.filterIsInstance<Category>().any { excluded in it.value }
        } ?: false

    override fun describe(): String {
        return "TaggedFilter(testCaseId=$filterTestCaseId, " +
                "category=$filterCategory, excludeCategory=$excludeCategory, " +
                "requiredCategory=$requiredCategory, " +
                "tagKey=$filterTagKey, tagValue=$filterTagValue, " +
                "rerunModeEnabled=$rerunModeEnabled, rerunAttempt=$rerunAttempt)"
    }
}
