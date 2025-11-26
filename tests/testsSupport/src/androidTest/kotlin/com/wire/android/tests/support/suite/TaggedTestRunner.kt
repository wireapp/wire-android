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

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom test runner that will later support filtering tests by:
 * - Category
 * - TestCaseId
 * - Tag key/value
 */
class TaggedTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        // Here we will later read runner arguments like:
        // -e category criticalFlow
        // -e testCaseId TC-8602
        // For now, just call super.
        super.onCreate(arguments)
    }
}
