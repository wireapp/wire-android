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
import com.wire.android.tests.support.tags.Tag
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.AssumptionViolatedException

class TagFilterRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val args = InstrumentationRegistry.getArguments()

                // We’ll pass these from Gradle
                val requestedKey = args.getString("tagKey")
                val requestedValue = args.getString("tagValue")

                // If nothing requested -> run everything normally
                if (requestedKey.isNullOrEmpty() || requestedValue.isNullOrEmpty()) {
                    base.evaluate()
                    return
                }

                // Collect @Tag from method + class
                val methodTag = description.getAnnotation(Tag::class.java)
                val classTag = description.testClass?.getAnnotation(Tag::class.java)

                val tagsForThisTest = listOfNotNull(methodTag, classTag)

                val hasRequestedTag = tagsForThisTest.any {
                    it.key == requestedKey && it.value == requestedValue
                }

                if (hasRequestedTag) {
                    // Tag matches -> run test
                    base.evaluate()
                } else {
                    // Tag does NOT match -> SKIP (not pass)
                    throw AssumptionViolatedException(
                        "[TagFilterRule] Skipping '${description.displayName}' " +
                                "because it does NOT have tag $requestedKey=$requestedValue"
                    )
                }
            }
        }
    }
}
