package com.wire.android

import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.Test
import java.time.LocalDateTime

class VersionizerTest: UnitTest() {

    @Test
    fun `given version generator when I generate a new version NOW then I should get an incremented version number`() {
        val versionCodeOld = Versionizer(LocalDateTime.now().minusSeconds(10)).versionCode
        val versionCodeNew = Versionizer().versionCode

        assertVersionCodes(versionCodeOld, versionCodeNew)
    }

    @Test
    fun `given version generator when I generate a new version TOMORROW then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusDays(1)).versionCode

        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN ONE YEAR then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusYears(1)).versionCode

        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN ONE and TWO YEARS then I should get an incremented version number`() {
        val now = LocalDateTime.now()
        val oldVersionCode = Versionizer(now.plusYears(1)).versionCode
        val newVersionCode = Versionizer(now.plusYears(2)).versionCode

        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN TEN YEARS then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusYears(10)).versionCode

        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    private fun assertVersionCodes(oldVersionCode: Int, newVersionCode: Int) {
        oldVersionCode shouldBeGreaterThan 0
        newVersionCode shouldBeGreaterThan 0
        oldVersionCode shouldBeLessThan newVersionCode
        oldVersionCode shouldBeLessThan Versionizer.MAX_VERSION_CODE_ALLOWED
    }
}
