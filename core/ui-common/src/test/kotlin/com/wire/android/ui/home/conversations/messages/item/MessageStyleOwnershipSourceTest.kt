/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations.messages.item

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageStyleOwnershipSourceTest {
    @Test
    fun givenMessageStyle_whenSourcesAreInspected_thenCoreOwnsTheStableFqnWithoutAppDependencies() {
        val coreSource = File(root, "core/ui-common/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageStyle.kt")
            .readText()

        assertTrue(coreSource.contains("package com.wire.android.ui.home.conversations.messages.item"))
        assertTrue(coreSource.contains("enum class MessageStyle"))
        assertFalse(coreSource.contains("import com.wire.android.R"))
        assertFalse(coreSource.contains("BuildConfig"))
        assertFalse(coreSource.contains("appLogger"))
        assertFalse(File(root, "app/src/main/kotlin/com/wire/android/ui/home/conversations/messages/item/MessageStyle.kt").exists())
    }

    private companion object {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
    }
}
