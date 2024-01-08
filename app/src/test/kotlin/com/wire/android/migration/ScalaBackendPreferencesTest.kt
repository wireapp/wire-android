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

package com.wire.android.migration

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.wire.android.migration.preference.ScalaBackendPreferences
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScalaBackendPreferencesTest {

    @Test
    fun `given regular url when fetching scala backend data then return this url`() = runTest {
        // given
        val blacklistUrl = "https://wire.com/blacklist"
        // when
        val (arrangement, scalaBackendPreferences) = Arrangement()
            .withSpecificPref(ScalaBackendPreferences.Companion.BLACKLIST_HOST_PREF, blacklistUrl)
            .arrange()
        // then
        assertEquals(blacklistUrl, scalaBackendPreferences.blacklistUrl)
    }

    @Test
    fun `given optional url when fetching scala backend data then extract proper url from the string`() = runTest {
        // given
        val blacklistUrl = "https://wire.com/blacklist"
        val optionalBlacklistUrl = "Some($blacklistUrl)"
        // when
        val (arrangement, scalaBackendPreferences) = Arrangement()
            .withSpecificPref(ScalaBackendPreferences.Companion.BLACKLIST_HOST_PREF, optionalBlacklistUrl)
            .arrange()
        // then
        assertEquals(blacklistUrl, scalaBackendPreferences.blacklistUrl)
    }

    @Test
    fun `given optional empty url when fetching scala backend data then return null`() = runTest {
        // given
        val optionalBlacklistUrl = "None"
        // when
        val (arrangement, scalaBackendPreferences) = Arrangement()
            .withSpecificPref(ScalaBackendPreferences.Companion.BLACKLIST_HOST_PREF, optionalBlacklistUrl)
            .arrange()
        // then
        assertEquals(null, scalaBackendPreferences.blacklistUrl)
    }

    private class Arrangement {

        @MockK
        lateinit var sharedPreferences: SharedPreferences
        @MockK
        lateinit var context: Context

        private val scalaBackendPreferences: ScalaBackendPreferences by lazy {
            ScalaBackendPreferences(context)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(PreferenceManager::class)
            every { PreferenceManager.getDefaultSharedPreferences(any()) } returns sharedPreferences
            every { sharedPreferences.getString(any(), any()) } returns null
        }

        fun withSpecificPref(key: String, value: String): Arrangement {
            every { sharedPreferences.getString(key, any()) } returns value
            return this
        }

        fun arrange() = this to scalaBackendPreferences
    }
}
