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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

internal class PdfDocumentRenderSizeTest {

    @Test
    fun `given a page that fits the budget, when sizing it, then the requested width is used`() {
        val (width, height) = renderSize(A4_WIDTH_PT, A4_HEIGHT_PT, requestedWidth = 1080)

        assertEquals(1080, width)
        assertAspectRatioPreserved(width, height)
    }

    @Test
    fun `given a tall page zoomed past the budget, when sizing it, then the aspect ratio is preserved`() {
        // The regression: clamping height alone squashed an A4 page once the requested width
        // pushed its height past the old 4096 px limit.
        val (width, height) = renderSize(A4_WIDTH_PT, A4_HEIGHT_PT, requestedWidth = 1080 * 3)

        assertAspectRatioPreserved(width, height)
        assertTrue(width < 1080 * 3, "expected the width to be scaled down, was $width")
    }

    @Test
    fun `given any zoom level, when sizing a page, then the pixel budget is respected`() {
        listOf(1080, 2160, 3240, 20_000).forEach { requestedWidth ->
            val (width, height) = renderSize(A4_WIDTH_PT, A4_HEIGHT_PT, requestedWidth)

            assertTrue(
                width.toLong() * height <= PdfDocument.MAX_RENDER_PIXELS,
                "budget exceeded at width $requestedWidth: ${width}x$height",
            )
        }
    }

    @Test
    fun `given a landscape page, when sizing it, then the aspect ratio is preserved`() {
        val (width, height) = renderSize(A4_HEIGHT_PT, A4_WIDTH_PT, requestedWidth = 4000)

        assertTrue(width > height, "landscape page should stay wider than tall, was ${width}x$height")
        assertAspectRatioPreserved(width, height, expected = A4_HEIGHT_PT.toFloat() / A4_WIDTH_PT)
    }

    @Test
    fun `given a degenerate page size, when sizing it, then the default ratio is used`() {
        val (width, height) = renderSize(pageWidth = 0, pageHeight = 0, requestedWidth = 1000)

        assertAspectRatioPreserved(width, height, expected = PdfDocument.DEFAULT_ASPECT_RATIO)
    }

    @Test
    fun `given a non positive requested width, when sizing it, then the bitmap is still valid`() {
        val (width, height) = renderSize(A4_WIDTH_PT, A4_HEIGHT_PT, requestedWidth = 0)

        assertTrue(width >= 1 && height >= 1, "bitmap dimensions must stay positive, was ${width}x$height")
    }

    private fun assertAspectRatioPreserved(
        width: Int,
        height: Int,
        expected: Float = A4_WIDTH_PT.toFloat() / A4_HEIGHT_PT,
    ) {
        val actual = width.toFloat() / height
        assertTrue(
            abs(actual - expected) <= RATIO_TOLERANCE,
            "expected ratio ~$expected but was $actual (${width}x$height)",
        )
    }

    private companion object {
        const val A4_WIDTH_PT = 595
        const val A4_HEIGHT_PT = 842

        // Integer rounding of both axes moves the ratio slightly.
        const val RATIO_TOLERANCE = 0.01f
    }
}
