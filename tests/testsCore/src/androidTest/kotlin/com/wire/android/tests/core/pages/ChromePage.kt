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
package com.wire.android.tests.core.pages

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class ChromePage(private val device: UiDevice) {

    private val useWithoutAccountLocator = UiSelectorParams(text = "Use without an account")

    fun clickUseWithoutAccount(): ChromePage {
        UiWaitUtils.waitElement(useWithoutAccountLocator).click()
        return this
    }

    private fun isInstalled(pkg: String): Boolean {
        val output = runShellCommand("pm list packages $pkg")
        return output.contains(pkg)
    }

    fun clearInstalledBrowsers() {
        listOf(
            "com.android.chrome",
            "app.vanadium.browser",
            "org.lineageos.jelly"
        )
            .filter(::isInstalled)
            .forEach { pkg ->
                val result = runShellCommand("pm clear $pkg")
                println("Cleared $pkg -> $result")
            }
    }

    private fun runShellCommand(command: String): String =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .executeShellCommand(command)
            .trim()
}
