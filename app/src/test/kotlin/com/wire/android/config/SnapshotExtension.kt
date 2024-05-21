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

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import java.lang.reflect.Method

/**
 * This extension is used to test [androidx.compose.foundation.text.input.TextFieldState] as it's a specific mutable state,
 * it's needed to manually accept changes to it, which in a running app is normally done by the compose runtime.
 * There is no official guide on how to write tests for the TextFieldState, but this is how it's done in the compose source code.
 * Take a look at: https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/foundation/foundation/src/androidUnitTest/kotlin/androidx/compose/foundation/text/input/TextFieldStateTest.kt#715
 */
@ExperimentalCoroutinesApi
class SnapshotExtension : InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>?,
        invocationContext: ReflectiveInvocationContext<Method>?,
        extensionContext: ExtensionContext?
    ) {
        val globalWriteObserverHandle = Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications() // This is normally done by the compose runtime.
        }
        try {
            invocation?.proceed()
        } finally {
            globalWriteObserverHandle.dispose()
        }
    }
}
