package com.wire.android.common

import com.wire.android.utils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()
}
