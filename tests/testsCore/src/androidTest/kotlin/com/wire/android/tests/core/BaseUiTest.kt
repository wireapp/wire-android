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
package com.wire.android.tests.core

import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.support.suite.CategoryFilterRule
import org.junit.Rule
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import com.wire.android.tests.support.suite.TestCaseIdFilterRule

/**
 * Base class for all UI tests.
 * - Starts Koin with testModule
 */
abstract class BaseUiTest : KoinTest {

    // Koin rule (starts dependency injection)
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(testModule)
    }

    // Category filter rule (allows -e category=...)
    @get:Rule
    val categoryFilterRule = CategoryFilterRule()

    // TestCaseId filter rule (allows -e testCaseId =... TC-8602)
    @get:Rule
    val testCaseIdFilterRule = TestCaseIdFilterRule()
}
