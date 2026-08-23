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

package com.wire.android.ui.common.groupname

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GroupNamePolicyTest {

    @Test
    fun `given whitespace-only text, when evaluated, then result is empty`() {
        assertEquals(GroupNamePolicyResult.Empty, GroupNamePolicy.evaluate(" \t\n ", ""))
    }

    @Test
    fun `given surrounding whitespace around original name, when evaluated, then result is unchanged`() {
        assertEquals(GroupNamePolicyResult.Unchanged, GroupNamePolicy.evaluate(" group name ", "group name"))
    }

    @Test
    fun `given original name itself has surrounding whitespace, when evaluated, then comparison remains exact`() {
        assertEquals(GroupNamePolicyResult.Valid, GroupNamePolicy.evaluate(" group name ", " group name "))
    }

    @Test
    fun `given changed non-empty text, when evaluated, then result is valid`() {
        assertEquals(GroupNamePolicyResult.Valid, GroupNamePolicy.evaluate("new group name", "group name"))
    }

    @Test
    fun `given exactly 64 characters, when evaluated, then result is valid`() {
        assertEquals(GroupNamePolicyResult.Valid, GroupNamePolicy.evaluate("a".repeat(64), ""))
    }

    @Test
    fun `given 65 characters, when evaluated, then result is too long`() {
        assertEquals(GroupNamePolicyResult.TooLong, GroupNamePolicy.evaluate("a".repeat(65), ""))
    }

    @Test
    fun `given supplementary characters, when evaluated, then UTF-16 char counting is preserved`() {
        assertEquals(GroupNamePolicyResult.Valid, GroupNamePolicy.evaluate("😀".repeat(32), ""))
        assertEquals(GroupNamePolicyResult.TooLong, GroupNamePolicy.evaluate("😀".repeat(33), ""))
    }

    @Test
    fun `given a too-long name equal to the original, when evaluated, then length takes precedence`() {
        val groupName = "a".repeat(65)

        assertEquals(GroupNamePolicyResult.TooLong, GroupNamePolicy.evaluate(groupName, groupName))
    }
}
