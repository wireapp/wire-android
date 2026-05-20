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
package com.wire.benchmark

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import kotlin.time.Duration.Companion.seconds

/**
 * Login to the app using the provided email and password. This function assumes that the app is on the login screen.
 */
fun MacrobenchmarkScope.login(email: String, password: String) {
    device.findObject(By.res("userIdentifierInput"))?.text = email
    device.findObject(By.res("loginButton"))?.click()
    device.findObject(By.res("PasswordInput"))?.text = password
    device.findObject(By.res("LoginNextButton"))?.click()
    waitForAnalyticsIfPresentAndAgree()
    device.wait(Until.hasObject(By.text("Conversations")), 30.seconds.inWholeMilliseconds)
}

/**
 * Wait for the analytics dialog to appear and click "Agree" if it is present.
 * This function assumes that the app is on the screen where the analytics dialog may appear.
 */
fun MacrobenchmarkScope.waitForAnalyticsIfPresentAndAgree() {
    device.wait(Until.hasObject(By.text("Agree")), 10.seconds.inWholeMilliseconds)
    device.findObject(By.text("Agree"))?.click()
}

fun MacrobenchmarkScope.switchBackend(backendConfigUrl: String) {
    val deepLinkUrl = "wire://access/?config=$backendConfigUrl"
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(deepLinkUrl)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    device.wait(Until.hasObject(By.text("Proceed")), 10.seconds.inWholeMilliseconds)
    device.findObject(By.text("Proceed"))?.click()
    device.wait(Until.hasObject(By.res("loginButton")), 30.seconds.inWholeMilliseconds)
}
