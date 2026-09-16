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
package com.wire.android.pdfviewer

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class PageBitmapCacheTest {

    @Test
    fun `given a cached page, when reading it back, then the same bitmap is returned`() {
        val cache = PageBitmapCache(maxBytes = 1_000)
        val page = bitmapOf(100)

        cache.put("0@100", page)

        assertSame(page, cache.get("0@100"))
        assertNull(cache.get("1@100"))
    }

    @Test
    fun `given the budget is exceeded, when adding a page, then the least recently used one is dropped`() {
        val cache = PageBitmapCache(maxBytes = 250)
        cache.put("0", bitmapOf(100))
        cache.put("1", bitmapOf(100))

        cache.put("2", bitmapOf(100))

        assertNull(cache.get("0"))
        assertNotNull(cache.get("1"))
        assertNotNull(cache.get("2"))
    }

    @Test
    fun `given a page was read recently, when the budget is exceeded, then the other one is dropped first`() {
        val cache = PageBitmapCache(maxBytes = 250)
        cache.put("0", bitmapOf(100))
        cache.put("1", bitmapOf(100))

        cache.get("0")
        cache.put("2", bitmapOf(100))

        assertNotNull(cache.get("0"))
        assertNull(cache.get("1"))
    }

    @Test
    fun `given a page larger than the whole budget, when adding it, then it is still served`() {
        val cache = PageBitmapCache(maxBytes = 10)
        val huge = bitmapOf(5_000)

        cache.put("0", huge)

        assertSame(huge, cache.get("0"))
        assertEquals(1, cache.size())
    }

    @Test
    fun `given a replaced page, when accounting for the budget, then the old size is released`() {
        val cache = PageBitmapCache(maxBytes = 250)
        cache.put("0", bitmapOf(200))
        cache.put("0", bitmapOf(100))

        cache.put("1", bitmapOf(100))

        assertNotNull(cache.get("0"))
        assertNotNull(cache.get("1"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `given cached pages, when clearing, then nothing is served and the budget is free again`() {
        val cache = PageBitmapCache(maxBytes = 250)
        cache.put("0", bitmapOf(200))

        cache.clear()

        assertNull(cache.get("0"))
        assertEquals(0, cache.size())
        cache.put("1", bitmapOf(200))
        assertNotNull(cache.get("1"))
    }

    private fun bitmapOf(bytes: Int): Bitmap = mockk<Bitmap>(relaxed = true).also {
        every { it.byteCount } returns bytes
    }
}
