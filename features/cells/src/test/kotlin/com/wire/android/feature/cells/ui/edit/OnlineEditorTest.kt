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
package com.wire.android.feature.cells.ui.edit

import com.wire.android.feature.cells.ui.edit.OnlineEditor.Companion.asReadOnlyEditorUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnlineEditorTest {

    @Test
    fun givenEditPermission_whenMakingReadOnly_thenOnlyPermissionChanges() {
        val url = "https://cells.example/browser/cool.html?host=wss://cells.example&permission=edit&lang=en"

        assertEquals(
            "https://cells.example/browser/cool.html?host=wss://cells.example&permission=read&lang=en",
            url.asReadOnlyEditorUrl()
        )
    }

    @Test
    fun givenNoPermission_whenMakingReadOnly_thenPermissionIsAdded() {
        assertEquals("https://cells.example/cool.html?a=1&permission=read", "https://cells.example/cool.html?a=1".asReadOnlyEditorUrl())
    }
}
