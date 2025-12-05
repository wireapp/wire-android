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

import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.Tag
import com.wire.android.tests.support.tags.TestCaseId
import io.qameta.allure.kotlin.Allure
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit Rule that converts our custom annotations into Allure tags.
 *
 * Everything is mapped to Allure's built-in "tag" label so it is clearly
 * visible in the Allure UI under the "Tags" section.
 *
 *  - @TestCaseId("TC-8602")      → tag: TC-8602
 *  - @Category("criticalFlow")   → tag: criticalFlow
 *  - @Tag(key="feature","calling") → tag: feature:calling
 */
class AllureLabelsRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val annotations = description.annotations

                // ---- TestCaseId → tag: TC-8602 ----
                annotations
                    .filterIsInstance<TestCaseId>()
                    .firstOrNull()
                    ?.let { anno ->
                        Allure.label("tag", anno.value)
                    }

                // ---- Category → tag: criticalFlow, regression, ... ----
                annotations
                    .filterIsInstance<Category>()
                    .forEach { anno ->
                        anno.value.forEach { cat ->
                            Allure.label("tag", cat)
                        }
                    }

                // ---- Tag → tag: feature:calling, etc. ----
                annotations
                    .filterIsInstance<Tag>()
                    .forEach { anno ->
                        Allure.label("tag", "${anno.key}:${anno.value}")
                    }

                // Run the actual test
                base.evaluate()
            }
        }
    }
}
