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

package com.wire.android.config

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * This extension provides a way to mock scoped ViewModel arguments getters.
 * It can be used to mock getting scoped ViewModel arguments from savedStateHandle, like: savedStateHandle.scopedArgs().
 *
 * Add this JUnit 5 extension to your test class using
 * @JvmField
 * @RegisterExtension
 * val scopedArgsTestExtension = ScopedArgsTestExtension()
 *
 * or:
 *
 * Annotating the class with
 * @ExtendWith(ScopedArgsTestExtension::class)
 */
@ExperimentalCoroutinesApi
class ScopedArgsTestExtension : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        mockkStatic("com.wire.android.di.ViewModelScopedKt")
    }

    override fun afterEach(context: ExtensionContext?) {
        unmockkStatic("com.wire.android.di.ViewModelScopedKt")
    }
}
