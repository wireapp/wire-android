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

import com.wire.android.gradle.version.Versionizer
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDateTime

@RunWith(JUnit4::class)
class VersionizerTest {

    @Test
    fun `given version generator when I generate two versions AT THE SAME TIME I should get the same version number`() {
        val dateTime = LocalDateTime.now()

        Versionizer(dateTime).versionCode shouldBeEqualTo Versionizer(dateTime).versionCode
    }


    @Test
    fun `given version generator when I generate a version I should get the same version number on the current Android project`() {
        val dateTime = LocalDateTime.of(2021, 6, 23, 13, 54, 28)

        Versionizer(dateTime).versionCode shouldBeEqualTo 548966
    }

    @Test
    fun `given before than 21 of June 2024 Build, then bump every 10 seconds`() {
        val oldDate = LocalDateTime.of(2024, 6, 20, 0, 0)
        val oldVersionCode = Versionizer(oldDate).versionCode
        val newVersionCode = Versionizer(oldDate.plusSeconds(10)).versionCode

        assertVersionCodeAreProperlyIncremented(oldVersionCode, newVersionCode)
        assertEquals(1, newVersionCode - oldVersionCode)
    }

    @Test
    fun `given after 21 of June 2024 Build, then bump every 5 minutes`() {
        val oldDate = LocalDateTime.of(2024, 6, 22, 14, 0)
        val oldVersionCode = Versionizer(oldDate).versionCode
        val newVersionCode = Versionizer(oldDate.plusMinutes(5)).versionCode

        println("Version number: $newVersionCode")
        assertVersionCodeAreProperlyIncremented(oldVersionCode, newVersionCode)
        assertEquals(1, newVersionCode - oldVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version THE NEXT DAY then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusDays(1)).versionCode

        assertVersionCodeAreProperlyIncremented(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN ONE YEAR then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusYears(1)).versionCode

        assertVersionCodeAreProperlyIncremented(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN ONE and TWO YEARS then I should get an incremented version number`() {
        val now = LocalDateTime.now()
        val oldVersionCode = Versionizer(now.plusYears(1)).versionCode
        val newVersionCode = Versionizer(now.plusYears(2)).versionCode

        assertVersionCodeAreProperlyIncremented(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN TEN YEARS then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusYears(10)).versionCode

        // This will break 655 years from now.
        assertVersionCodeAreProperlyIncremented(oldVersionCode, newVersionCode)
    }

    private fun assertVersionCodeAreProperlyIncremented(oldVersionCode: Int, newVersionCode: Int) {
        oldVersionCode shouldBeGreaterThan 0
        newVersionCode shouldBeGreaterThan 0
        oldVersionCode shouldBeLessThan newVersionCode
        oldVersionCode shouldBeLessThan Versionizer.MAX_VERSION_CODE_ALLOWED
    }
}
