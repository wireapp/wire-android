/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.tests.core.messages

abstract class WireMessage(msg: String) : BackendMessage(msg) {
    companion object {
        const val ZETA_CODE_HEADER_NAME = "X-Zeta-Code"
        internal const val ZETA_KEY_HEADER_NAME = "X-Zeta-Key"
        const val ZETA_PURPOSE_HEADER_NAME = "X-Zeta-Purpose"
    }

    val xZetaPurpose: String?
        get() = getHeaderValue(ZETA_PURPOSE_HEADER_NAME)

    protected abstract val expectedPurposeValue: String

    val isValid: Boolean
        get() = try {
            xZetaPurpose == expectedPurposeValue
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
}
