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
package com.wire.android.emm

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidUserContextProviderTest {

    @Test
    fun `given UID in main user range, then user ID should be 0`() {
        val provider = FakeAndroidUserContextProvider(uid = 10123) // UID 10123 / 100000 = 0
        assertEquals(0, provider.getCurrentAndroidUserId())
        assertEquals("0", provider.getCurrentUserIdKey())
    }

    @Test
    fun `given UID in work profile range, then user ID should be 10`() {
        val provider = FakeAndroidUserContextProvider(uid = 1010123) // UID 1010123 / 100000 = 10
        assertEquals(10, provider.getCurrentAndroidUserId())
        assertEquals("10", provider.getCurrentUserIdKey())
    }

    @Test
    fun `given UID at exact boundary, then user ID should be calculated correctly`() {
        val provider = FakeAndroidUserContextProvider(uid = 100000) // UID 100000 / 100000 = 1
        assertEquals(1, provider.getCurrentAndroidUserId())
        assertEquals("1", provider.getCurrentUserIdKey())
    }

    @Test
    fun `given UID zero, then user ID should be 0`() {
        val provider = FakeAndroidUserContextProvider(uid = 0)
        assertEquals(0, provider.getCurrentAndroidUserId())
        assertEquals("0", provider.getCurrentUserIdKey())
    }

    @Test
    fun `given UID just below boundary, then user ID should be 0`() {
        val provider = FakeAndroidUserContextProvider(uid = 99999)
        assertEquals(0, provider.getCurrentAndroidUserId())
        assertEquals("0", provider.getCurrentUserIdKey())
    }

    @Test
    fun `given DEFAULT_KEY constant, then it should be 'default'`() {
        assertEquals("default", AndroidUserContextProvider.DEFAULT_KEY)
    }
}

/**
 * Fake implementation for testing that allows injecting a specific UID value.
 */
class FakeAndroidUserContextProvider(private val uid: Int) : AndroidUserContextProvider {
    override fun getCurrentAndroidUserId(): Int = uid / AndroidUserContextProvider.UID_DIVISOR
    override fun getCurrentUserIdKey(): String = getCurrentAndroidUserId().toString()
}
