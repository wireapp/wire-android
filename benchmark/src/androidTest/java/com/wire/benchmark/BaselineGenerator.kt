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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME
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
            it.text = EMAIL
        }
        device.findObject(By.res("PasswordInput"))?.let {
            it.text = PASSWORD
        }
        device.findObject(By.res("loginButton"))?.let {
            it.click()
        }
        device.wait(Until.hasObject(By.text("Conversations")), 30_000)
    }

    companion object {
        private const val PACKAGE_NAME = "com.wire.android.internal"
        private const val EMAIL = ""
        private const val PASSWORD = ""
    }
}
