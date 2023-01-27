/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.utils

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.lifecycle.ViewModel
import org.junit.rules.TestRule
import kotlin.reflect.KClass

/**
 * Convenience function to obtain a ViewModel instance from the AndroidComposeTestRule
 */
inline fun <reified VM : ViewModel, R : TestRule, A : ComponentActivity>
AndroidComposeTestRule<R, A>.getViewModel(viewModel: KClass<VM>): VM {
    println("Getting test instance of ViewModel: $viewModel")
    return this.activity.viewModels<VM>().value
}

inline fun <reified VM : ViewModel> getViewModel(activity: ComponentActivity, viewModel: KClass<VM>): VM {
    println("Getting test instance of ViewModel: $viewModel")
    return activity.viewModels<VM>().value
}

fun ComposeTestRule.waitForExecution(timeoutMillis: Long = WAIT_TIMEOUT, block: () -> Unit) {
    waitUntil(timeoutMillis) {
        try {
            block()
            true
        } catch (exception: Throwable) {
            false
        }
    }
}

const val WAIT_TIMEOUT = 9000L
val EMAIL = System.getProperty("EMAIL").orEmpty() // TODO: extract from adb using: `adb your-launch-command -e SOME_KEY some_value`
val PASSWORD = System.getProperty("PASSWORD").orEmpty() // TODO: extract from adb using: `adb your-launch-command -e SOME_KEY some_value`
val USER_NAME = System.getProperty("USER_NAME").orEmpty() // TODO: extract from adb using: `adb your-launch-command -e SOME_KEY some_value`
val EMAIL_2 = EMAIL
val PASSWORD_2 = PASSWORD
