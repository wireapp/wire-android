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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class BaselineGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val args get() = InstrumentationRegistry.getArguments()
    private val targetPackage get() = args.getString("TARGET_PACKAGE", "com.wire")
    private val email get() = args.getString("EMAIL").orEmpty()
    private val password get() = args.getString("PASSWORD").orEmpty()


    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = targetPackage
    ) {
        pressHome()
        startActivityAndWait()
        login()
    }

    private fun MacrobenchmarkScope.login() {
        device.findObject(By.res("loginButton"))?.let {
            it.click()
        }
        device.findObject(By.res("userIdentifierInput"))?.let {
            it.text = email
        }
        device.findObject(By.res("PasswordInput"))?.let {
            it.text = password
        }
        device.findObject(By.res("loginButton"))?.let {
            it.click()
        }
        device.wait(Until.hasObject(By.text("Agree")), 10.seconds.inWholeMilliseconds)
        device.findObject(By.text("Agree"))?.let {
            it.click()
        }
        device.wait(Until.hasObject(By.text("Conversations")), 30.seconds.inWholeMilliseconds)
    }
}
