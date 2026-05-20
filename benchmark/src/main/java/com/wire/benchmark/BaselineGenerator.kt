/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val args get() = InstrumentationRegistry.getArguments()
    private val targetPackage get() = args.getString("TARGET_PACKAGE", "com.wire")
    private val email get() = args.getString("EMAIL").orEmpty()
    private val password get() = args.getString("PASSWORD").orEmpty()
    private val conversationName get() = args.getString("CONVERSATION_NAME").orEmpty()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = targetPackage,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        if (email.isNotEmpty() && password.isNotEmpty()) {
            login(email, password)
            openContactsAndReturn()
            openConversation(conversationName)
        }
    }
}
