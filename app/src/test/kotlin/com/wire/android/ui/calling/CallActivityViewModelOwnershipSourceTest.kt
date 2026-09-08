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
package com.wire.android.ui.calling

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CallActivityViewModelOwnershipSourceTest {

    @Test
    fun givenCallActivityViewModel_whenInspectingCreation_thenActivityOwnsIt() {
        val activity = sourceFile("CallActivity.kt").readText()
        val bindings = sourceFile("CallingMetroViewModelBindings.kt").readText()

        assertTrue(activity.contains("CallActivityViewModel by viewModels"))
        assertTrue(activity.contains("Provider<CallActivityViewModel>"))
        assertFalse(activity.contains("wireMetroViewModel<CallActivityViewModel>"))
        assertFalse(bindings.contains("@ViewModelKey(CallActivityViewModel::class)"))
    }

    private fun sourceFile(name: String): File {
        val relative = "src/main/kotlin/com/wire/android/ui/calling/$name"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }
}
