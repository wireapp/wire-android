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

const val WAIT_TIMEOUT = 9000L
const val EMAIL = "mustafa+1@wire.com"
const val PASSWORD = "Mustafastaging1!"
const val USER_NAME = "Mustafastaging1"
const val EMAIL_2 = "mustafa+7@wire.com"
const val PASSWORD_2 = "mustafa+7@wire.com"
const val USER_NAME_2 = "doga7"
