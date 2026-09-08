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

package com.wire.android.ui.home.meetings

import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MeetingsScreenSourceTest {

    @Test
    fun `editing a meeting opens the typed Navigation 3 edit flow`() {
        val source = sourceFile().readText().filterNot(Char::isWhitespace)

        assertTrue("navigationActions:MeetingsHomeNavigationActions" in source)
        assertTrue(
            "editMeeting={meetingId->navigationActions.openNewMeeting(NewMeetingType.Edit(meetingId))}," in source
        )
    }

    private fun sourceFile(): File =
        generateSequence(File(checkNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .map { File(it, "app/src/main/kotlin/com/wire/android/ui/home/meetings/MeetingsScreen.kt") }
            .firstOrNull(File::isFile)
            ?: error("Unable to locate MeetingsScreen.kt")
}
