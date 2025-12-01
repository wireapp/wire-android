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
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit Rule that filters tests based on @Category annotations.
 *
 * Usage:
 * - Add @get:Rule val categoryFilterRule = CategoryFilterRule()
 * - Run tests with instrumentation arg: -e category criticalFlow
 */
class CategoryFilterRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getArguments()
                val selectedCategory = instrumentation.getString("category")

                // If no category filter passed → run normally
                if (selectedCategory.isNullOrEmpty()) {
                    base.evaluate()
                    return
                }

                // Collect categories from annotations on method + class
                val methodCategories = description.annotations
                    .filterIsInstance<Category>()
                    .flatMap { it.value.toList() }

                val classCategories = description.testClass
                    ?.annotations
                    ?.filterIsInstance<Category>()
                    ?.flatMap { it.value.toList() }
                    ?: emptyList()

                val allCategories = methodCategories + classCategories

                // If selected category matches → run test
                if (allCategories.contains(selectedCategory)) {
                    base.evaluate()
                } else {
                    println("Skipping test '${description.methodName}' because it does NOT match category '$selectedCategory'")
                }
            }
        }
    }
}
