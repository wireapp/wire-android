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

/**
 * Least-recently-used cache of rendered pages, bounded by the total bitmap size in bytes rather
 * than by a page count, because page bitmaps vary a lot in size.
 *
 * Deliberately not [android.util.LruCache]: that one is stubbed out in unit tests, and the byte
 * accounting is the part worth covering.
 */
internal class PageBitmapCache(private val maxBytes: Long) {

    private val entries = LinkedHashMap<String, Bitmap>(0, LOAD_FACTOR, true)
    private var currentBytes = 0L

    @Synchronized
    fun get(key: String): Bitmap? = entries[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        entries.put(key, bitmap)?.let { replaced -> currentBytes -= replaced.byteCount }
        currentBytes += bitmap.byteCount
        trimToSize()
    }

    @Synchronized
    fun clear() {
        entries.clear()
        currentBytes = 0
    }

    @Synchronized
    fun size(): Int = entries.size

    /** Drops the least recently used entries until the cache fits again, always keeping the newest. */
    private fun trimToSize() {
        val iterator = entries.entries.iterator()
        while (currentBytes > maxBytes && entries.size > 1 && iterator.hasNext()) {
            currentBytes -= iterator.next().value.byteCount
            iterator.remove()
        }
    }

    companion object {
        private const val LOAD_FACTOR = 0.75f
        private const val HEAP_FRACTION = 8

        /** Roughly an eighth of the heap, the same budget the platform LRU caches usually take. */
        fun defaultMaxBytes(): Long = (Runtime.getRuntime().maxMemory() / HEAP_FRACTION).coerceAtLeast(1)
    }
}
