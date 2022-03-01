package com.wire.android.utils

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass
import org.junit.rules.TestRule

/**
 * Convenience function to obtain a ViewModel instance from the AndroidComposeTestRule
 */
inline fun <reified VM : ViewModel, R : TestRule, A : ComponentActivity> AndroidComposeTestRule<R, A>.getViewModel(viewModel: KClass<VM>): VM { // ktlint-disable max-line-length
    println("Getting test instance of ViewModel: $viewModel")
    return this.activity.viewModels<VM>().value
}
