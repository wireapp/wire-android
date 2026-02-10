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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@RunWith(JUnit4::class)
class RenameApkTaskTest {

    @Test
    fun `given single output when naming apk then legacy name format is used`() {
        val fileName = RenameApkTask.buildLegacyApkFileName(
            applicationId = "com.wire.android",
            versionName = "4.21.0-73661-internal",
            buildType = "compat",
            isUniversalOutput = false,
        )

        assertEquals(
            "com.wire.android-v4.21.0-73661-internal-compat.apk",
            fileName
        )
    }

    @Test
    fun `given universal output when naming apk then universal suffix is included`() {
        val fileName = RenameApkTask.buildLegacyApkFileName(
            applicationId = "com.wire.android",
            versionName = "4.21.0-73661-internal",
            buildType = "compat",
            isUniversalOutput = true,
        )

        assertEquals(
            "com.wire.android-v4.21.0-73661-internal-compat-universal.apk",
            fileName
        )
    }
}
