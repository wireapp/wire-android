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

package com.wire.android

import com.wire.kalium.logger.KaliumLogLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireApplicationLoggingConfigTest {

    @Test
    fun `given minimal logger config then warn level and single sink are used`() {
        val config = WireApplication.minimalLoggerConfig()

        assertEquals(KaliumLogLevel.WARN, config.logLevel)
        assertEquals(1, config.initialLogWriterList.size)
    }

    @Test
    fun `given full logger config when logging disabled then fallback matches minimal logger`() {
        val config = WireApplication.fullLoggerConfig(isLoggingEnabled = false)

        assertEquals(KaliumLogLevel.WARN, config.logLevel)
        assertEquals(1, config.initialLogWriterList.size)
    }

    @Test
    fun `given full logger config when logging enabled then verbose level and two sinks are used`() {
        val config = WireApplication.fullLoggerConfig(isLoggingEnabled = true)

        assertEquals(KaliumLogLevel.VERBOSE, config.logLevel)
        assertEquals(2, config.initialLogWriterList.size)
    }
}
