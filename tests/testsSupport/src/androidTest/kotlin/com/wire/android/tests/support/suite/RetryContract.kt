/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

/**
 * Shared retry contract between CI and instrumentation code.
 *
 * Test IDs must be in this exact format:
 *   com.example.ClassName#testMethodName
 */
object RetryContract {
    const val ARG_ENABLE_RERUN_MODE = "enableRerunMode"
    const val ARG_RERUN_ATTEMPT = "rerunAttempt"
    const val ARG_RERUN_LIST_PATH = "rerunListPath"
    const val ARG_RERUN_LIST_INLINE = "rerunListInline"
    const val ARG_RERUN_LIST_INLINE_PART_PREFIX = "rerunListInlinePart"

    const val ALLURE_LABEL_PASSED_ON_RERUN = "passed_on_rerun"
}
