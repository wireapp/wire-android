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

package com.wire.android.ui.authentication.verificationcode

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationCodeStateTest {

    @Test
    fun givenDefaultState_thenEveryValueMatchesThePresentationContract() {
        val state = VerificationCodeState()

        assertEquals(6, state.codeLength)
        assertEquals("", state.emailUsed)
        assertFalse(state.isCodeInputNecessary)
        assertFalse(state.isCurrentCodeInvalid)
        assertNull(state.remainingTimerText)
        assertEquals(6, VerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH)
    }

    @Test
    fun givenAllValues_whenCopyingState_thenDataClassEqualityAndFieldOrderArePreserved() {
        val expected = VerificationCodeState(
            codeLength = 8,
            emailUsed = "member@example.com",
            isCodeInputNecessary = true,
            isCurrentCodeInvalid = true,
            remainingTimerText = "00:42",
        )
        val copied = VerificationCodeState().copy(
            codeLength = 8,
            emailUsed = "member@example.com",
            isCodeInputNecessary = true,
            isCurrentCodeInvalid = true,
            remainingTimerText = "00:42",
        )
        val codeLength = copied.codeLength
        val emailUsed = copied.emailUsed
        val isCodeInputNecessary = copied.isCodeInputNecessary
        val isCurrentCodeInvalid = copied.isCurrentCodeInvalid
        val remainingTimerText = copied.remainingTimerText

        assertEquals(expected, copied)
        assertEquals(expected.hashCode(), copied.hashCode())
        assertEquals(8, codeLength)
        assertEquals("member@example.com", emailUsed)
        assertTrue(isCodeInputNecessary)
        assertTrue(isCurrentCodeInvalid)
        assertEquals("00:42", remainingTimerText)
    }

    @Test
    fun givenMovedState_thenFeatureOwnsItsPreservedFqn() {
        val repositoryRoot = repositoryRoot()
        val sourcePath = "com/wire/android/ui/authentication/verificationcode/VerificationCodeState.kt"
        val legacySource = repositoryRoot.resolve("app/src/main/kotlin/$sourcePath")
        val featureSource = repositoryRoot.resolve("features/authentication/src/main/kotlin/$sourcePath")

        assertFalse(Files.exists(legacySource), "Legacy source still exists: $legacySource")
        assertTrue(Files.isRegularFile(featureSource), "Missing feature source: $featureSource")
        assertEquals(
            "com.wire.android.ui.authentication.verificationcode.VerificationCodeState",
            VerificationCodeState::class.java.name,
        )
        assertTrue(
            Files.readString(featureSource).contains(
                "package com.wire.android.ui.authentication.verificationcode"
            )
        )
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }
}
