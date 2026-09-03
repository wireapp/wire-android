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

package com.wire.android.ui

import android.app.Application
import android.app.ComponentCaller
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WireActivityNewIntentCallerTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun givenAndroid14_whenReplacingIntent_thenUsesLegacyIntentSetter() {
        val activity = mockk<AppCompatActivity>(relaxed = true)
        val intent = Intent(Intent.ACTION_SEND)

        activity.setIntentPreservingCaller(intent)

        verify(exactly = 1) { activity.setIntent(intent) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun givenAndroid15_whenReplacingIntent_thenRetainsCurrentCaller() {
        val caller = mockk<ComponentCaller>()
        val activity = mockk<AppCompatActivity>(relaxed = true) {
            every { currentCaller } returns caller
        }
        val intent = Intent(Intent.ACTION_SEND)

        activity.setIntentPreservingCaller(intent)

        verify(exactly = 1) { activity.setIntent(intent, caller) }
        verify(exactly = 0) { activity.setIntent(intent) }
    }
}
