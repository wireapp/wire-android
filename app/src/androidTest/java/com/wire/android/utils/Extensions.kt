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
val EMAIL = System.getProperty("EMAIL").toString() // TODO: extract from adb using: `adb your-launch-command -e SOME_KEY some_value`
val PASSWORD = System.getProperty("PASSWORD").toString() // TODO: extract from adb using: `adb your-launch-command -e SOME_KEY some_value`
val USER_NAME = System.getProperty("USER_NAME").toString() // TODO: extract from adb using: `adb your-launch-command -e SOME_KEY some_value`
val EMAIL_2 = EMAIL
val PASSWORD_2 = PASSWORD
