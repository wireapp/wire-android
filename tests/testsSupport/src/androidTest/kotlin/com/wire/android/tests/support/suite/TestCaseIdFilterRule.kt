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
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit Rule that filters tests based on @TestCaseId.
 *  - Run tests with: -e testCaseId TC-8602
 */
class TestCaseIdFilterRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val args = InstrumentationRegistry.getArguments()
                val requestedId = args.getString("testCaseId")

                // If no argument provided -> run everything normally
                if (requestedId.isNullOrEmpty()) {
                    base.evaluate()
                    return
                }

                // Look for @TestCaseId on the METHOD
                val methodAnno = description.getAnnotation(TestCaseId::class.java)
                // And optionally on the CLASS
                val classAnno = description.testClass?.getAnnotation(TestCaseId::class.java)

                val idsForThisTest = listOfNotNull(methodAnno?.value, classAnno?.value)

                if (idsForThisTest.contains(requestedId)) {
                    // Matching ID -> run the test
                    base.evaluate()
                } else {
                    // Not matching -> skip
                    println(
                        "[TestCaseIdFilterRule] Skipping '${description.displayName}' " +
                                "because it does NOT match testCaseId '$requestedId'"
                    )
                }
            }
        }
    }
}
