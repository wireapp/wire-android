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
import android.util.Log
import io.qameta.allure.android.runners.AllureAndroidJUnitRunner

/**
 * Custom test runner that delegates to Allure's Android runner,
 * and filters tests by @TestCaseId, @Category, and @Tag BEFORE
 * they are executed (and before Allure sees them).
 */

class TaggedTestRunner : AllureAndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle) {
        val filterId = arguments.getString("testCaseId")
        val category = arguments.getString("category")
        val tagKey = arguments.getString("tagKey")
        val tagValue = arguments.getString("tagValue")

        Log.i(
            "TaggedTestRunner",
            "onCreate called. " +
                    "testCaseId=$filterId, category=$category, tagKey=$tagKey, tagValue=$tagValue"
        )

        super.onCreate(arguments)
    }
}
