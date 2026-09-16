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
package com.wire.android.util

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import com.wire.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CommonIntentUtilTest {

    @Test
    fun `given a geo handler when opening location then no fallback or toast is used`() {
        val context = LocationIntentContext(failedLaunches = 0)

        openLocation(context)

        assertEquals(1, context.launchedIntents.size)
        assertEquals(Intent.ACTION_VIEW, context.launchedIntents.single().action)
        assertEquals("geo", context.launchedIntents.single().data?.scheme)
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `given only a browser handler when opening location then fallback opens without a toast`() {
        val context = LocationIntentContext(failedLaunches = 1)

        openLocation(context)

        assertLocationLaunchOrder(context)
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `given no handlers when opening location then a short toast is shown without crashing`() {
        val context = LocationIntentContext(failedLaunches = 2)

        openLocation(context)

        assertLocationLaunchOrder(context)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(
            context.getString(R.string.label_no_application_found_open_location),
            ShadowToast.getTextOfLatestToast()
        )
        assertEquals(Toast.LENGTH_SHORT, ShadowToast.getLatestToast().duration)
    }

    private fun openLocation(context: Context) {
        launchGeoIntent(52.5f, 13.4f, "Berlin", FALLBACK_URL, context)
    }

    private fun assertLocationLaunchOrder(context: LocationIntentContext) {
        assertEquals(listOf(Intent.ACTION_VIEW, Intent.ACTION_VIEW), context.launchedIntents.map { it.action })
        assertEquals("geo", context.launchedIntents.first().data?.scheme)
        assertEquals(FALLBACK_URL, context.launchedIntents.last().data.toString())
    }

    private class LocationIntentContext(
        private val failedLaunches: Int
    ) : ContextWrapper(ApplicationProvider.getApplicationContext<Application>()) {
        val launchedIntents = mutableListOf<Intent>()

        override fun startActivity(intent: Intent) {
            launchedIntents.add(intent)
            if (launchedIntents.size <= failedLaunches) {
                throw ActivityNotFoundException()
            }
        }
    }

    private companion object {
        const val FALLBACK_URL = "https://maps.google.com/maps?z=16&q=loc:52.5+13.4"
    }
}
