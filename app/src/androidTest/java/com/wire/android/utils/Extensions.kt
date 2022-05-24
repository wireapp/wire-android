package com.wire.android.utils

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass
import org.junit.rules.TestRule

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

const val WAIT_TIMEOUT = 5000L
const val PASSWORD = "Mustafastaging1!"
const val EMAIL = "mustafa+4@wire.com"
const val USER_NAME = "doga4"
