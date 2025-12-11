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

package com.wire.android.ui.home.conversations.model.messagetypes.image

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.wire.android.ui.WireTestTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VisualMediaParamsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenNonPositiveRealDimensions_shouldReturnMinSizeAndLandscapeFlag() = runTest {
        val params = VisualMediaParams(realMediaWidth = 0, realMediaHeight = -10)

        val minW = 40.dp
        val minH = 30.dp

        var result: NormalizedSize? = null

        composeTestRule.setContent {
            WireTestTheme {
                result = params.normalizedSize(
                    minW = minW,
                    minH = minH,
                    maxBounds = MaxBounds.DpBounds(
                        maxW = 200.dp,
                        maxH = 200.dp
                    )
                )
            }
        }

        composeTestRule.runOnIdle {
            val size = result!!
            assertEquals(minW, size.width)
            assertEquals(minH, size.height)
            assertFalse(size.isPortrait)
        }
    }

    @Test
    fun givenLandscapeImageAndDpBounds_shouldFitWithinMaxAndRespectAspectRatio() = runTest {
        val params = VisualMediaParams(realMediaWidth = 1920, realMediaHeight = 1080)

        val minW = 40.dp
        val minH = 40.dp
        val maxW = 300.dp
        val maxH = 300.dp

        var result: NormalizedSize? = null

        composeTestRule.setContent {
            WireTestTheme {
                result = params.normalizedSize(
                    minW = minW,
                    minH = minH,
                    maxBounds = MaxBounds.DpBounds(
                        maxW = maxW,
                        maxH = maxH
                    )
                )
            }
        }

        composeTestRule.runOnIdle {
            val size = result!!

            assertTrue(size.width <= maxW)
            assertTrue(size.height <= maxH)

            assertTrue(size.width >= minW)
            assertTrue(size.height >= minH)

            assertFalse(size.isPortrait)
        }
    }

    @Test
    fun givenPortraitImageAndDpBounds_shouldFitWithinMaxAndRespectAspectRatio() = runTest {
        val params = VisualMediaParams(realMediaWidth = 1080, realMediaHeight = 1920)

        val minW = 40.dp
        val minH = 40.dp
        val maxW = 300.dp
        val maxH = 300.dp

        var result: NormalizedSize? = null

        composeTestRule.setContent {
            WireTestTheme {
                result = params.normalizedSize(
                    minW = minW,
                    minH = minH,
                    maxBounds = MaxBounds.DpBounds(
                        maxW = maxW,
                        maxH = maxH
                    )
                )
            }
        }

        composeTestRule.runOnIdle {
            val size = result!!

            assertTrue(size.width <= maxW)
            assertTrue(size.height <= maxH)

            assertTrue(size.width >= minW)
            assertTrue(size.height >= minH)

            assertTrue(size.isPortrait)
        }
    }

    @Test
    fun givenMinWidthGreaterThanMaxWidth_shouldNotCrashAndClampToMax() = runTest {
        val params = VisualMediaParams(realMediaWidth = 2000, realMediaHeight = 1000)

        val minW = 300.dp
        val minH = 150.dp
        val maxW = 200.dp
        val maxH = 120.dp

        var result: NormalizedSize? = null

        composeTestRule.setContent {
            WireTestTheme {
                result = params.normalizedSize(
                    minW = minW,
                    minH = minH,
                    maxBounds = MaxBounds.DpBounds(
                        maxW = maxW,
                        maxH = maxH
                    )
                )
            }
        }

        composeTestRule.runOnIdle {
            val size = result!!

            assertEquals(maxW, size.width)

            assertTrue(size.height > 0.dp)
            assertTrue(size.height <= maxH)
        }
    }

    @Test
    fun givenScreenFractionBounds_shouldStayWithinCalculatedMaxBounds() = runTest {
        val params = VisualMediaParams(realMediaWidth = 1920, realMediaHeight = 1080)

        val minW = 80.dp
        val minH = 80.dp
        val fractionW = 0.2f
        val fractionH = 0.2f

        var result: NormalizedSize? = null

        composeTestRule.setContent {
            WireTestTheme {
                result = params.normalizedSize(
                    minW = minW,
                    minH = minH,
                    maxBounds = MaxBounds.ScreenFraction(
                        maxWFraction = fractionW,
                        maxHFraction = fractionH
                    )
                )
            }
        }

        composeTestRule.runOnIdle {
            val size = result!!

            assertTrue(size.width > 0.dp)
            assertTrue(size.height > 0.dp)
        }
    }
}
